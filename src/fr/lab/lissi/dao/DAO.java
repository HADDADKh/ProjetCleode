package fr.lab.lissi.dao;

import java.util.List;

public interface DAO<T> {
	
	public List<T> getAllOffredData();
	
	public List<T> getAllOffredActions();
	
	
	
}
