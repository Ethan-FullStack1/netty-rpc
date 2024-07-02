package com.rpc.netty.rapid.invoke.consumer.test;

import lombok.Data;

@Data
public class User {
	
	public User() {
	}

	public User(String id, String name) {
		this.id = id;
		this.name = name;
	}

	private String id;
	
	private String name;
	
}
