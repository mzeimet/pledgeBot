package mzeimet.telegram_bot;

public class PledgeReservation {

	private String name;
	private int userNr;
	private int pledgeOrderNr;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getUserNr() {
		return userNr;
	}

	public void setUserNr(int userNr) {
		this.userNr = userNr;
	}

	public int getPledgeOrderNr() {
		return pledgeOrderNr;
	}

	public void setPledgeOrderNr(int pledgeOrderNr) {
		this.pledgeOrderNr = pledgeOrderNr;
	}

	public PledgeReservation(String name, int userNr, int pledgeOrderNr) {
		this.name = name;
		this.userNr = userNr;
		this.pledgeOrderNr = pledgeOrderNr;
	}
	
	public String toString(){
		return name  + "," + userNr + "," + pledgeOrderNr;
	}

}
