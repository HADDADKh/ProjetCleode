package fr.lab.lissi.controller;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import fr.lab.lissi.dao.DaoAbstract;
import fr.lab.lissi.dao.DaoImpl;
import fr.lab.lissi.model.Data;
import fr.lab.lissi.model.DataFormatter;

@Path("/")
public class GenericService {

	protected DaoAbstract<?> dao = new DaoImpl();

	@GET
	@Path("/data")
	@Produces(MediaType.APPLICATION_JSON)
	public String offredData() {
		return DataFormatter.formatToJson(dao.getAllOffredData());
	}

	@GET
	@Path("/actions")
	@Produces(MediaType.APPLICATION_JSON)
	public String offredActions() {
		return DataFormatter.formatToJson(dao.getAllOffredActions());
	}

	@GET
	@Path("/data/{dataId}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getById(@PathParam("dataId") String id) {
		return DataFormatter.formatToJson((Data) dao.getById(id));
	}

	@GET
	@Path("/data/getAll")
	@Produces(MediaType.APPLICATION_JSON)
	public String getRandomNumber() {
		List<Data> dataList = new DaoImpl().getAll();
		return DataFormatter.formatDataToJson(dataList);
	}

	// @GET
	// @Path("/data/{dataId}/inscription")
	// @Produces(MediaType.APPLICATION_JSON)
	// public String subscription(@PathParam("dataId") String id) {
	//
	// return DataFormatter.formatToJson("inscription");
	// }

	@GET
	@Path("/actions/publish/presence")
//	@Produces(MediaType.APPLICATION_JSON)
	public Response publishRandomNumberIn() {
		/*
		 * Il vaut mieux donner comme paramètre : http://localhost, 6791, Variables
		 */

		if((Data)dao.getById("tagid") == null) return null;
		
		String tagid = ((Data)dao.getById("tagid")).getValue();
		
		if(tagid.isEmpty()) return null;
		
		Client client = ClientBuilder.newClient();
		WebTarget target = client.target("http://localhost:6791").path("Variables");


		Form form = new Form();
		form.param("PresenceDetected", tagid);

		Response resp = target.request(MediaType.TEXT_PLAIN).post(
				Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

		System.out.println("resp-->" + resp.getStatusInfo());

//		target = target.queryParam("action", "getvariables").queryParam("variables", "numberList");
//		String s = target.request(MediaType.TEXT_PLAIN_TYPE).get(String.class);
//
//		System.out.println(target.getUri() + " --> " + URLDecoder.decode(s));
		return Response.status(200).build();
	}
}
