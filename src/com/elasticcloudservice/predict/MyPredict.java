package com.elasticcloudservice.predict;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyPredict {
	
	//方法一：最后几天
	public static Map<String, Integer> lastDay(List<String> flavour, int[][] train){
		Map<String, Integer> map = new HashMap<String, Integer>();
		int len = train[0].length;
		if(len > 2){
			for(int i=0; i<flavour.size(); i++){
				int n = (int)Math.ceil(0.7 * train[i][0]  + 0.3 * train[i][1] + 0.2 * train[i][2]);
				if(n < 0){
					n = Math.abs(n);
				}
				map.put(flavour.get(i), n);
			}
		}
		else{
			for(int i=0; i<flavour.size(); i++){
				map.put(flavour.get(i), (int)1.2 * train[i][0]);
			}
		}
		
		return map;
	}
	
	//方法二：二次移动
	public static Map<String, Integer> twiceMove(List<String> flavour, int[][] train){
		Map<String, Integer> map = new HashMap<String, Integer>();
		int[] result = new int[flavour.size()];
		int n = train[0].length;//每种flavor有n组数据
		//#######
		//System.out.println(n);
		if(n < 5){
			map = lastDay(flavour, train);
		}
		else{
			//二次移动实现
			for(int i=0; i<flavour.size(); i++){
				double[] m1 = new double[n];
				double[] m2 = new double[n];
				int x = 0;
				for(int j=n-1; j>=2; j--){
					m1[x++] = (train[i][j] + train[i][j-1] + train[i][j-2])/3.0;
				}
				for(int j=0; j<n-4; j++){
					m2[j] = (m1[j]+m1[j+1]+m1[j+2])/3.0;
				}
				
				result[i] = (int)Math.round(3*m1[n-3] - 2*m2[n-5]);
				if(result[i] < 0){
					result[i] = 0;
				}
				map.put(flavour.get(i), result[i]);
			}
		}
		
		return map;
	}
	
	
	//方法三：GRU
	public static Map<String, Integer> gru(List<String> flavour, int[][] train){
		Map<String, Integer> map = new HashMap<>();
		
		int len = train.length;
		int n = train[0].length;
		if(n < 5){
			map = lastDay(flavour, train);
		}
		else{
			int tn = 5;//只要前五个数据
			int m = 3;//3个节点单元
			int max = 0;
			for(int i=0; i<len; i++){
				for(int j=0; j<tn; j++){
					if(train[i][j] > max){
						max = train[i][j];
					}
				}
			}
			
			double[][] train0 = new double[len][n];
			for(int i=0; i<len; i++){
				for(int j=0; j<tn; j++){
					train0[i][j] = train[i][j]/(double)max;
				}
			}
			
			double[] ht0 = new double[len];
			double[] ht = new double[len];
			double[] xt = new double[len];
			double[] xt1 = new double[len];
			
			double[] zt = new double[len];
			double[] rt = new double[len];
			double[] htm = new double[len];
			
			//3个节点单元
			double[][][] wz = new double[len][3][2];
			double[][][] wr = new double[len][3][2];
			double[][][] w = new double[len][3][2];
			for(int i=0; i<len; i++){
				for(int j=0; j<m; j++){
					for(int k=0; k<2; k++){
						wz[i][j][k] = Math.random();
						wr[i][j][k] = Math.random();
						w[i][j][k] = Math.random();
					}
				}
			}
			
			int k = 1000;//训练次数
			double delta = 0.9;//学习率 
			while(k-- > 0){
				for(int i=0; i<len; i++){
					ht0[i] = train0[i][tn-1];			
				}
				
				//每种flavour
				for(int i=0; i<len; i++){
					
					//一轮训练次数
					for(int j=0; j<m; j++){
						xt[i] = train0[i][tn-2-j];
						xt1[i] = train0[i][tn-3-j];
						//计算ht
						zt[i] = HelpGru.sigmoid(wz[i][j][0] * ht0[i] + wz[i][j][1] * xt[i]);
						rt[i] = HelpGru.sigmoid(wr[i][j][0] * ht0[i] + wr[i][j][1] * xt[i]);
						htm[i] = HelpGru.tanh(w[i][j][0] * rt[i] * ht0[i] + w[i][j][1] * xt[i]);
						ht[i] = (1-zt[i]) * ht0[i] + zt[i] * htm[i];
						
						//反向计算
						double dh = xt1[i] - ht[i];
						double errh = (htm[i] - ht0[i]) * dh + zt[i];
						
						double errhtm0 = HelpGru.retanh(htm[i]) * errh * rt[i] * ht0[i];
						w[i][j][0] += delta * errhtm0 * htm[i];
						double errhtm1 = HelpGru.retanh(htm[i]) * errh * xt[i];
						w[i][j][1] += delta *errhtm1 * htm[i];
						
						double errrt0 = HelpGru.resigmoid(rt[i]) * errhtm0 * ht0[i];
						wr[i][j][0] += delta * errrt0 * rt[i];
						double errrt1 = HelpGru.resigmoid(rt[i]) * errhtm1 * xt[i];
						wr[i][j][1] += delta * errrt1 * rt[i];
						
						double errzt0 = HelpGru.resigmoid(zt[i]) * errrt0 * ht0[i];
						wz[i][j][0] += delta * errzt0 * rt[i];
						double errzt1 = HelpGru.resigmoid(zt[i]) * errrt1 * xt[i];
						wz[i][j][1] += delta * errzt1 * rt[i];
						
						//完了之后
						ht0[i] = ht[i];
					}
		
				}	
			}
			
			//预测
			for(int i=0; i<len; i++){
				ht0[i] = train0[i][tn-2];
				
				for(int j=0; j<m; j++){
					
					xt[i] =train[i][tn-3-j];
					zt[i] = HelpGru.sigmoid(wz[i][j][0] * ht0[i] + wz[i][j][1] * xt[i]);
					rt[i] = HelpGru.sigmoid(wr[i][j][0] * ht0[i] + wr[i][j][1] * xt[i]);
					htm[i] = HelpGru.tanh(w[i][j][0] * rt[i] * ht0[i] + w[i][j][1] * xt[i]);
					ht[i] = (1-zt[i]) * ht0[i] + zt[i] * htm[i];
					
					ht0[i] = ht[i];
				}
				
				int tm = (int)Math.round(ht[i] * max);
				map.put(flavour.get(i), tm);
			}
			
		}
		
		return map;
	}
	
	
	
	
	
	//方法四：
	
	
}
