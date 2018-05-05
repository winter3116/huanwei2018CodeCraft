package com.elasticcloudservice.predict;

import static java.lang.Math.exp;

public class HelpGru {
	//四个激活函数
	static double sigmoid(double x){
		return 1.0/(1.0 + exp(-x));
	}
	
	static double resigmoid(double y){
		return y * (1.0 - y);
	}
	
	static double tanh(double x){
		return (exp(x) - exp(-x))/(exp(x) + exp(-x));
	}
	
	static double retanh(double y){
		return 1.0 - y*y;
	}
	
	
	
	

}
