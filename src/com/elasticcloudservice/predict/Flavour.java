package com.elasticcloudservice.predict;

public class Flavour {
	public String name;
	public int cpu;
	public int mem;
	
	Flavour(String name, int cpu, int mem){
		this.name = name;
		this.cpu = cpu;
		this.mem = mem;
	}
	
	public int getCpu(){
		return cpu;
	}
	
	public int getMem(){
		return mem;
	}

}
