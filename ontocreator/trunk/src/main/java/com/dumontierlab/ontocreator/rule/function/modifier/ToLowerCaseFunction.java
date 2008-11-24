package com.dumontierlab.ontocreator.rule.function.modifier;

import java.util.ArrayList;
import java.util.List;

import com.dumontierlab.ontocreator.rule.function.Function;
import com.dumontierlab.ontocreator.rule.function.RuntimeFunctionException;

public class ToLowerCaseFunction implements Function {

	public List<String> apply(List<String> input) throws RuntimeFunctionException {
		List<String> results = new ArrayList<String>();
		for (String inputString : input) {
			results.add(inputString.toLowerCase());
		}
		return results;
	}

}
