package com.elasticcloudservice.predict;

import java.util.HashMap;
import java.util.Map;

public class Server {
	public int id;
	public int cpu;
	public int mem;
	Map<String, Integer> map = new HashMap<String, Integer>();
	
	Server(int id, int cpu, int mem){
		this.id = id;
		this.cpu = cpu;
		this.mem = mem;
	}
}
