package com.dumontierlab.jxta.owl.jxta;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.wsdl.WSDLException;
import javax.xml.parsers.ParserConfigurationException;

import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.XMLDocument;
import net.jxta.document.XMLElement;
import net.jxta.exception.PeerGroupException;
import net.jxta.peergroup.NetPeerGroupFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.rendezvous.RendezVousService;
import net.jxta.soap.SOAPService;
import net.jxta.soap.SOAPServiceThread;
import net.jxta.soap.ServiceDescriptor;

import org.apache.axis.wsdl.fromJava.Emitter;
import org.bouncycastle.util.encoders.UrlBase64;
import org.xml.sax.SAXException;

import com.dumontierlab.jxta.owl.jxta.exception.JxtaBootstrapException;
import com.dumontierlab.jxta.owl.jxta.exception.JxtaException;

public class JxtaServiceImpl implements JxtaService {

	private static final Logger LOG = Logger.getLogger(JxtaServiceImpl.class.getName());

	private final String peerName;
	private final String jxtaHome;
	private final Collection<URI> seedsUris;

	private RendezVousService rendezvous;
	private PeerGroup netPeerGroup;

	public JxtaServiceImpl(String peerName, Collection<URI> seedsUris, String jxtaHome) throws JxtaBootstrapException {
		this.peerName = peerName;
		this.seedsUris = seedsUris;
		this.jxtaHome = jxtaHome;
		startJxta();
	}

	public PeerGroup getPeerGroup() {
		return netPeerGroup;
	}

	public void advertiseSoapService(ServiceDescriptor serviceDescriptor) throws JxtaException {
		// TODO: make lifetime and expiration a parameter for this method.
		SOAPService service = new SOAPService();

		// Build the ModuleSpecAdv 'Param' section
		XMLDocument param = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(new MimeMediaType("text/xml"),
				"Parm");

		// *************** 1. Add the service WSDL description *****************
		XMLElement wsdlElem;
		try {
			wsdlElem = param.createElement("WSDL", new String(UrlBase64.encode(generateWSDL(serviceDescriptor)
					.getBytes())));
			param.appendChild(wsdlElem);
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Unable to generate WSDL for service: " + serviceDescriptor.getName(), e);
		}

		// ********************* 2. Add secure pipe tag ************************
		String secure = (serviceDescriptor.isSecure()) ? "true" : "false";
		XMLElement secureElem = param.createElement("secure", secure);
		param.appendChild(secureElem);

		// Set the context object, with init parameters
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("serviceName", serviceDescriptor.getName());
		service.setContext(parameters);

		// Initialize the service
		try {
			service.init(netPeerGroup, serviceDescriptor, param, null);
		} catch (Exception e) {
			throw new JxtaException("Unable to initialize service: " + serviceDescriptor.getName(), e);
		}
		// saving msadv
		try {
			param.sendToStream(new FileOutputStream("ServiceParm.xml"));
		} catch (Exception e) {
			throw new JxtaException("Exception in saving service params section", e);
		}

		// Start a new ServiceThread running this service
		new SOAPServiceThread(service).start();

	}

	private String generateWSDL(ServiceDescriptor descriptor) throws IOException, WSDLException, SAXException,
			ParserConfigurationException, ClassNotFoundException {

		Emitter emitter = new Emitter();
		emitter.setCls(descriptor.getClassname());
		emitter.setServiceElementName(descriptor.getName());
		emitter.setLocationUrl("http://localhost:8080/axis/services/" + descriptor.getName());
		return emitter.emitToString(Emitter.MODE_ALL);
	}

	protected void startJxta() throws JxtaBootstrapException {
		NetworkConfigurator configuration = new NetworkConfigurator();
		File home = new File(jxtaHome, peerName);
		configuration.setHome(home);

		boolean needToConfigure = configuration.exists();

		if (!needToConfigure) {
			try {
				configuration.load();
			} catch (Exception e) {
				LOG.log(Level.WARNING, "Unable to load configuration, generating new configuration.", e);
				needToConfigure = true;
			}
		}

		if (needToConfigure) {
			configuration.setName(peerName);
			// TODO: What is the right node type to use?
			configuration.setMode(NetworkConfigurator.RDV_NODE);
			configuration.setUseMulticast(true);
			for (URI seed : seedsUris) {
				configuration.addRdvSeedingURI(seed);
			}

			try {
				configuration.save();
			} catch (IOException e) {
				LOG.log(Level.WARNING, "Unable to save peer configuration.", e);
			}
		}

		try {
			NetPeerGroupFactory factory = new NetPeerGroupFactory(configuration.getPlatformConfig(), home.toURI());
			netPeerGroup = factory.getInterface();
			rendezvous = netPeerGroup.getRendezVousService();

			LOG.info("My JXTA PeerID is: " + netPeerGroup.getPeerID().getUniqueValue().toString());

		} catch (PeerGroupException e) {
			throw new JxtaBootstrapException("Unable to bootstrap JXTA: " + e.getMessage(), e);
		}

	}

}