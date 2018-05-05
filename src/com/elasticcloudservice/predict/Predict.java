package com.elasticcloudservice.predict;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.filetool.util.LogUtil;

public class Predict {

	public static String[] predictVm(String[] ecsContent, String[] inputContent) {
		
		/** =========do your work here========== **/
		String[] str1 = inputContent[0].split(" ");
		int sCpu = Integer.valueOf(str1[0]);
		int sMem = Integer.valueOf(str1[1]);
		int fNum = Integer.valueOf(inputContent[2]);//flavour种类
		Map<String, Flavour> mapFla = new HashMap<String, Flavour>();
		List<String> flavour = new ArrayList<String>();//每种flavour的名字
		for(int i=0, j=3; i<fNum; i++, j++){
			String fName = inputContent[j].split(" ")[0];
			int fCpu = Integer.valueOf(inputContent[j].split(" ")[1]);
			int fMem = (Integer.valueOf(inputContent[j].split(" ")[2]))/1024;
			flavour.add(fName);
			mapFla.put(fName, new Flavour(fName, fCpu, fMem));
		}
		
		//int inputLength = inputContent.length;
		String flag = inputContent[fNum + 4];	//装箱时对那种资源要求，CPU或MEM
		//timeLength为所要求预测的时间天数
		String beginDate = inputContent[fNum + 6].split(" ")[0].split("-")[2];
		String endDate = inputContent[fNum + 7].split(" ")[0].split("-")[2];
		int timeLength = (Integer.valueOf(endDate) - Integer.valueOf(beginDate));
		if(timeLength < 0){
			timeLength = timeLength + 30;
		}
		
		
		//year-month-day
		String start_str = ecsContent[0].split("\t")[2].split(" ")[0];
		int ecsLen = ecsContent.length;
		String strEnd;
		do{
			strEnd = ecsContent[ecsLen-1];
			ecsLen--;
		}while("".equals(strEnd.trim()));
		String end_str = ecsContent[ecsLen].split("\t")[2].split(" ")[0];
		int[] start = {Integer.valueOf(start_str.split("-")[0]), Integer.valueOf(start_str.split("-")[1]), Integer.valueOf(start_str.split("-")[2])};
		int[] end = {Integer.valueOf(end_str.split("-")[0]), Integer.valueOf(end_str.split("-")[1]), Integer.valueOf(end_str.split("-")[2])};
		String[] time = new String[100];
		
		//将训练数据划分为n段
		int n = 0;
		//########
		//System.out.println(timeLength);
		//System.out.println(end_str + "," + start_str);
		//########
		String time_str = end_str;//时间点是从后到前
		do{
			time[n] = time_str;
			n++;
			int[] temp = {Integer.valueOf(time_str.split("-")[0]), Integer.valueOf(time_str.split("-")[1]), Integer.valueOf(time_str.split("-")[2])};
			int year = temp[0];
			int month = temp[1];
			int day = temp[2];
			//######
			//System.out.println(year+","+month+","+day);
			day = day - timeLength;
			if(day < 1){
				day = day + 30;
				month = month - 1;
			}
			if(month < 1){
				month = month + 12;
				year = year - 1;
			}
			time_str = year + "-" + String.format("%02d", month) + "-" + String.format("%02d", day);
		}while(time_str.compareTo(start_str) >= 0);
		//######
		//System.out.println(n+","+time_str);
		//######
		//每种flavor每一时间段的数量
		int[][] train = new int[fNum][n-1];
		for(int i=0; i<fNum; i++){
			for(int j=0; j<n-1; j++){
				train[i][j] = 0;
			}
		}
		//将escContent中的数据进行统计,train[j]时间反序
		for (int i = ecsLen; i >= 0; i--) {

			if ((ecsContent[i].split("\t").length == 3)
					&&flavour.contains(ecsContent[i].split("\t")[1])) {

				for(int j=0; j<fNum; j++){
					if(ecsContent[i].split("\t")[1].equals(flavour.get(j))){
						for(int k=0; k<n-1; k++){
							if((ecsContent[i].split("\t")[2].split(" ")[0].compareTo(time[k])<=0)
									&&(ecsContent[i].split("\t")[2].split(" ")[0].compareTo(time[k+1])>0)){
								train[j][k]++;
							}
						}
					}
					
				}
			}
		}
		//上面是数据处理，下面是预测和装箱
		
		//预测#####
		Map<String, Integer> map = new HashMap<>();
		
		//预测1：最后几天
		map = MyPredict.lastDay(flavour, train);
		
		//预测2：二次移动
		//map = MyPredict.twiceMove(flavour, train);
		
		//预测3：gru
		//map = MyPredict.gru(flavour, train);
		
		Map<String, Integer> map0 = new HashMap<>(map);//一个副本
		
		
		//System.out.println("##############");
		//LogUtil.printLog("Mid");
		//装箱#####
		List<Server> servers = new ArrayList<Server>();
		
		//装箱1:首次适应
		//servers = MyDistrubute.firstFit(map, mapFla, sCpu, sMem);
		
		//装箱2：由大到小
		//servers = MyDistrubute.bts(map, mapFla, sCpu, sMem, flag);
		
		//装箱3：模拟退火1,一个一个箱装
		//servers = MyDistrubute.mnth1(map, mapFla, sCpu, sMem, flag);
		
		//装箱4：模拟退火2+ff
		servers = MyDistrubute.mnth2(map, mapFla, sCpu, sMem, flag);
		
		//输出数组
		int sum = 0;//flavour数量
		for(String key : map0.keySet()){
			sum += map0.get(key);
		}

		String[] results = new String[ecsContent.length];
		results[0] = new Integer(sum).toString();
		for(int i=1; i<=fNum; i++){
			results[i] = flavour.get(i-1) + " " + map0.get(flavour.get(i-1));
		}
		results[fNum+1] = "\r";
		results[fNum+2] = new Integer(servers.size()).toString();
		for(int i=0; i<servers.size(); i++){
			Server ser = servers.get(i);
			String str = new Integer(ser.id).toString();
			for(String key : ser.map.keySet()){
				if(ser.map.get(key) != 0){
					str += " " + key + " " + ser.map.get(key);
				}
			}
			results[fNum+3+i] = str;
					
		}

		return results;
	}

}

