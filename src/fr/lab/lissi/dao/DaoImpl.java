package fr.lab.lissi.dao;

import java.util.List;

import cleode.Cleodes;
import cleode.TestZCL;
import fr.lab.lissi.model.Data;

public class DaoImpl extends DaoAbstract<Data> {

	@Override
	public Data getById(String dataId) {

		
		
		switch (dataId.toLowerCase()) {
		case "zplugenergy":
			
			System.out.println("Battery case - DaoImpl invoqued with "+dataId);
						
			
			System.out.println("recuperate data....".toUpperCase());
			//String yourData = "1298";
			System.out.println("data recuperated".toUpperCase());
			
			long data= Cleodes.getZplugEnergy();
			
			return new Data("integer", "watt", dataId, data+"");
		default:
			return new Data("none", "none", "none", "none");
		}
	}

	@Override
	public List<Data> getAll() {
		// TODO Auto-generated method stub
		return null;
	}

}
