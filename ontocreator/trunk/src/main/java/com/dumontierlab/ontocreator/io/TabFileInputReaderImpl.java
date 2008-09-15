package com.dumontierlab.ontocreator.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.dumontierlab.ontocreator.model.RecordImpl;
import com.dumontierlab.ontocreator.model.RecordSet;
import com.dumontierlab.ontocreator.model.RecordSetImpl;

public class TabFileInputReaderImpl implements InputReader {

	private final String delimeter;

	public TabFileInputReaderImpl(String _delimeter) {
		delimeter = _delimeter;
	}

	public RecordSet read(InputStream input) {

		RecordSetImpl rset = new RecordSetImpl("1");
		RecordImpl newRecord;
		String[] recordFields;

		try {
			BufferedReader breader = new BufferedReader(new InputStreamReader(
					input));

			String newLine = breader.readLine();

			while (newLine != null) {
				recordFields = newLine.split(delimeter);

				newRecord = new RecordImpl(recordFields[0]);

				for (int i = 0; i < recordFields.length; i++) {
					newRecord.addField(Integer.toString(i), recordFields[i]);
				}

				rset.addRecord(newRecord);

				newLine = breader.readLine();
			}

			breader.close();

		} catch (IOException e) {

			e.printStackTrace();
		}

		return rset;
	}

}
