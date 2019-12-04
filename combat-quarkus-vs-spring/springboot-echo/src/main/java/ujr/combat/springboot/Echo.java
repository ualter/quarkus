package ujr.combat.springboot;

public class Echo {
	
	private String uuid;
	private String word;
	private String ip;
	private String tag;
	
	public Echo(String uuid, String word, String ip) {
		super();
		this.uuid = uuid;
		this.word = word;
		this.ip   = ip;
		this.tag  = "SpringBoot";
	}
	public Echo() {
	}
	public String getUuid() {
		return uuid;
	}
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	public String getWord() {
		return word;
	}
	public void setWord(String word) {
		this.word = word;
	}
	public String getTag() {
		return tag;
	}
	public void setTag(String tag) {
		this.tag = tag;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}

}
