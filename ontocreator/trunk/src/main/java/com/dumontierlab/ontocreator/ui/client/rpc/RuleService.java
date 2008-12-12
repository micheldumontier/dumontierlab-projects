package com.dumontierlab.ontocreator.ui.client.rpc;

import com.dumontierlab.ontocreator.ui.client.rpc.exception.RuleServiceException;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("RuleService")
public interface RuleService extends RemoteService {

	String createInstanceMapping() throws RuleServiceException;

	String createClassMapping() throws RuleServiceException;

	String createBoundMapping(String uri) throws RuleServiceException;

	String addABoxQueryFilter(String ruleName, String query) throws RuleServiceException;

	String addTBoxQueryFilter(String ruleName, String queryType, String query) throws RuleServiceException;

	String addDataPropertyRegex(String ruleName, String propertyUri, String regex) throws RuleServiceException;

	String addClassAssertion(String ruleName, String description) throws RuleServiceException;

	void apply(String ruleName) throws RuleServiceException;

	class Util {
		private static RuleServiceAsync instance;

		public static RuleServiceAsync getInstance() {
			if (instance == null) {
				instance = (RuleServiceAsync) GWT.create(RuleService.class);
			}
			return instance;
		}
	}
}
