package fr.lab.lissi.model;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

public class DataFormatter {

	private static Gson gson = new Gson();

	public static String formatToJson(Data data) {

		List<Data> dataList = new ArrayList<Data>();
		dataList.add(data);

		JsonResponse response = new JsonResponse(new StampTime(12, 13), dataList);

		return gson.toJson(response);
	}

	public static String formatDataToJson(List<Data> dataList) {

		JsonResponse response = new JsonResponse(new StampTime(12, 13), dataList);

		return gson.toJson(response);
	}

	public static String formatToJson(List<String> allOffredData) {
		return gson.toJson(allOffredData);
	}

	public static String formatToJson(String string) {
		// TODO Auto-generated method stub
		return gson.toJson(string);
	}

}
