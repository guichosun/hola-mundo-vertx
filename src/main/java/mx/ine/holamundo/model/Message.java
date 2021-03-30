package mx.ine.holamundo.model;


import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

public class Message {

	private Integer id;

    private String messsage;

    public Message(Integer id, String messsage) {
    	super();
    	this.id = id;
    	this.messsage = messsage;
    }
    
    public Message() {
    	super();
    }
    
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getMesssage() {
		return messsage;
	}

	public void setMesssage(String messsage) {
		this.messsage = messsage;
	}


	@Override
	public String toString() {
		return "Message [id=" + id + ", messsage=" + messsage + "]";
	}

}
