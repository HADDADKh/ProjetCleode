package fr.lab.lissi.model;

public class SubscriptionRequest {
	private String URLContact;
	private String frequency;

	public SubscriptionRequest() {
	}

	public String getURLContact() {
		return URLContact;
	}

	public void setURLContact(String uRLContact) {
		URLContact = uRLContact;
	}

	public String getFrequency() {
		return frequency;
	}

	public void setFrequency(String frequency) {
		this.frequency = frequency;
	}
}
