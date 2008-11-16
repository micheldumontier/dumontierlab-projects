package com.dumontierlab.ontocreator.ui.server.rpc;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.mindswap.pellet.owlapi.PelletReasonerFactory;
import org.semanticweb.owl.inference.OWLReasoner;
import org.semanticweb.owl.inference.OWLReasonerException;
import org.semanticweb.owl.inference.OWLReasonerFactory;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyManager;

import com.dumontierlab.ontocreator.ui.client.model.OWLClassBean;
import com.dumontierlab.ontocreator.ui.client.model.TreeNode;
import com.dumontierlab.ontocreator.ui.client.rpc.OntologyService;
import com.dumontierlab.ontocreator.ui.server.rpc.util.ContinousRpcServlet;
import com.dumontierlab.ontocreator.ui.server.session.ClientSession;
import com.dumontierlab.ontocreator.ui.server.session.SessionHelper;

public class OntologyServiceImpl extends ContinousRpcServlet implements OntologyService {

	private final OWLReasonerFactory reasonerFactory = new PelletReasonerFactory();

	public Set<String> getLoadedOntologies() {
		ClientSession session = getClientSession();
		Set<String> ontologyUris = new HashSet<String>();
		for (OWLOntology ontology : session.getOntologies()) {
			ontologyUris.add(ontology.getURI().toString());
		}

		return ontologyUris;
	}

	public TreeNode<OWLClassBean> getClassHierarchy() {
		ClientSession session = getClientSession();
		OWLOntologyManager ontologyManager = session.getOntologyManager();
		OWLReasoner reasoner = null;
		TreeNode<OWLClassBean> tree = null;

		synchronized (reasonerFactory) {
			reasoner = reasonerFactory.createReasoner(ontologyManager);
		}
		try {
			reasoner.loadOntologies(session.getOntologies());
			reasoner.classify();
			tree = createTaxonomyTree(ontologyManager.getOWLDataFactory().getOWLThing(), reasoner);
		} catch (OWLReasonerException e) {
			throw new RuntimeException("Ontology classification failed.", e);
		}
		return tree;
	}

	private TreeNode<OWLClassBean> createTaxonomyTree(OWLClass concept, OWLReasoner reasoner)
			throws OWLReasonerException {

		TreeNode<OWLClassBean> root = new TreeNode<OWLClassBean>(createOWLClassBean(concept, reasoner));
		for (Set<OWLClass> children : reasoner.getSubClasses(concept)) {
			for (OWLClass subclass : children) {
				if (!subclass.equals(concept)) {
					TreeNode<OWLClassBean> node = createTaxonomyTree(subclass, reasoner);
					root.addChild(node);
				}
			}
		}
		return root;
	}

	private OWLClassBean createOWLClassBean(OWLClass concept, OWLReasoner reasoner) throws OWLReasonerException {
		OWLClassBean classBean = new OWLClassBean();
		classBean.setUri(concept.getURI().toString());
		classBean.setLabel(concept.getURI().toString());
		classBean.setUnsatisfiable(!reasoner.isSatisfiable(concept));
		return classBean;
	}

	private ClientSession getClientSession() {
		HttpServletRequest request = getThreadLocalRequest();
		return SessionHelper.getClientSession(request);
	}
}
