package fr.lab.lissi.dao;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import fr.lab.lissi.general.Constants;

public abstract class DaoAbstract<T> implements DAO<String> {

	@Override
	public List<String> getAllOffredData() {
		Properties myProperties = new Properties();
		try {
			myProperties.load(new FileInputStream(Constants.CONFIG_FILE_PATH));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Arrays.asList(myProperties.getProperty("offredDataList").trim().replaceAll(" ", "")
				.split(","));
	}

	@Override
	public List<String> getAllOffredActions() {
		Properties myProperties = new Properties();
		try {
			myProperties.load(new FileInputStream(Constants.CONFIG_FILE_PATH));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Arrays.asList(myProperties.getProperty("offredActionList").trim().replaceAll(" ", "")
				.split(","));
	}

	public abstract T getById(String id);

	public abstract List<T> getAll();

}
