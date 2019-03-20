package fr.lab.lissi.model;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import javax.xml.bind.annotation.XmlRootElement;

import com.google.gson.Gson;

import fr.lab.lissi.general.Constants;

@XmlRootElement
public class JsonResponse {

	private String deviceID;
	private String deviceLocation;
	private StampTime stampTime;
	private List<Data> dataList;

	public JsonResponse() {
	}

	public JsonResponse(String deviceID, String deviceLocation, StampTime stampTime,
			List<Data> dataList) {
		this.deviceID = deviceID;
		this.deviceLocation = deviceLocation;
		this.stampTime = stampTime;
		this.dataList = dataList;
	}

	public JsonResponse(StampTime stampTime, List<Data> dataList) {
		this.stampTime = stampTime;
		this.dataList = dataList;
		init();
	}
	
	private void init(){
		Properties myProperties = new Properties();
		try {
			myProperties.load(new FileInputStream(Constants.CONFIG_FILE_PATH));
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.deviceID = myProperties.getProperty("deviceID").trim();
		this.deviceLocation = myProperties.getProperty("deviceLocation").trim();;
	}

	public JsonResponse(String deviceID, String deviceLocation) {
		this.deviceID = deviceID;
		this.deviceLocation = deviceLocation;
	}

	public String getSensorID() {
		return deviceID;
	}

	public void setSensorID(String sensorID) {
		this.deviceID = sensorID;
	}

	public String getSensorLocation() {
		return deviceLocation;
	}

	public void setSensorLocation(String sensorLocation) {
		this.deviceLocation = sensorLocation;
	}

	public StampTime getStampTime() {
		return stampTime;
	}

	public void setStampTime(StampTime stampTime) {
		this.stampTime = stampTime;
	}

	public List<Data> getDataList() {
		return dataList;
	}

	public void setDataList(List<Data> dataList) {
		this.dataList = dataList;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return new Gson().toJson(this);
	}

}