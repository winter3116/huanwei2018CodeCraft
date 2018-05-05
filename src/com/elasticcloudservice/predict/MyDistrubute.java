package com.elasticcloudservice.predict;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class MyDistrubute {
	
	//方法一：首次适应
	public static List<Server> firstFit(Map<String, Integer> map, Map<String, Flavour> mapFla, int sCpu, int sMem){
		List<Server> servers = new ArrayList<Server>();
		int sNum = 0;
		for(String flavour : map.keySet()){
			int fCpu = mapFla.get(flavour).cpu;
			int fMem = mapFla.get(flavour).mem;
			
			if(map.get(flavour) == 0){
				continue;
			}
			if(sNum == 0){
				Server server = new Server(sNum+1, sCpu, sMem);
				servers.add(server);
				sNum++;		
			}
			for(int j=0; j<map.get(flavour); j++){
				boolean find = false;
				for(Server ser : servers){
					if(ser.cpu >= fCpu && ser.mem >= fMem){
						ser.cpu -= fCpu;
						ser.mem -= fMem;
						if(ser.map.containsKey(flavour)){
							int num = ser.map.remove(flavour);
							num++;
							ser.map.put(flavour, num);
						}
						else{
							ser.map.put(flavour, 1);
						}
						find = true;
						break;
					}
					
				}
				if(!find){
					Server server = new Server(sNum+1, sCpu, sMem);
					servers.add(server);
					sNum++;
					j--;
				}
			}
			
		}
		
		
		return servers;
	}
	
	
	//方法二：从大到小
	public static List<Server> bts(Map<String, Integer> map, Map<String, Flavour> mapFla, int sCpu, int sMem, String flag){
		List<Server> servers = new ArrayList<>();
		
		List<Flavour> flavours = new ArrayList<>();//保存所有flavor
		for(String key : map.keySet()){
			for(int i=0; i<map.get(key); i++){
				flavours.add(mapFla.get(key));
			}
		}
		//将flavour排序
		if("CPU".equals(flag)){
			flavours.sort(Comparator.comparing(Flavour::getCpu));
		}
		else{
			flavours.sort(Comparator.comparing(Flavour::getMem));
		}
		
		servers = ff(flavours, sCpu, sMem);
		
		return servers;
	}
	
	
	//方法三：模拟退火,一个箱一个箱装
	public static List<Server> mnth1(Map<String, Integer> map, Map<String, Flavour> mapFla, int sCpu, int sMem, String flag){
		List<Server> servers = new ArrayList<>();
		int sNum = 0;//server数量
		int tSum = 0;//flavour数量
		String[] str = new String[map.size()];
		int m = 0;
		for(String key : map.keySet()){
			tSum += map.get(key);
			str[m] = key;
			m++;
		}
		
		while(tSum > 0){
				
			double t = 100.0;//初始温度
			double tMin = 1e-4;//终止温度
			int k = 1000;//内循环迭代次数
			double delta = 0.99; //温度下降率
				
			//System.out.println(tSum);
			Server server = new Server(sNum+1, sCpu, sMem);
			Map<String, Integer> tmap = new HashMap<>();//每个server的解	
			for(String key : map.keySet()){
				tmap.put(key, 0);
			}
				
			//计算fx
			
			int fx = 0;
			if("CPU".equals(flag)){
				for(String key : tmap.keySet()){
					fx = fx + tmap.get(key) * mapFla.get(key).cpu;
				}
			}
			else{
				for(String key : tmap.keySet()){
					fx = fx + tmap.get(key) * mapFla.get(key).mem;
				}
			}
			//模拟退火
			while(t > tMin){
				for(int i=0; i<k; i++){
					//产生一个新解并计算fx0
					Map<String, Integer> ttmap = new HashMap<>(tmap);//每个server的解	
					
					int index = new Random().nextInt(map.size());
					if(0 == ttmap.get(str[index])){
						ttmap.put(str[index], ttmap.get(str[index])+1);
					}
					else if(ttmap.get(str[index]) == map.get(str[index])){
						ttmap.put(str[index], ttmap.get(str[index])-1);
					}
					else{
						if(Math.random() > 0.5){
							ttmap.put(str[index], ttmap.get(str[index])+1);
						}
						else{
							ttmap.put(str[index], ttmap.get(str[index])-1);
						}
					}
						
					//对新解检查
					boolean f = true;
					int tCpu = 0;
					int tMem = 0;
					for(String key : ttmap.keySet()){
						if(ttmap.get(key)<0 || ttmap.get(key)>map.get(key)){
							f = false;
							break;
						}
						tCpu += ttmap.get(key) * mapFla.get(key).cpu;
						tMem += ttmap.get(key) * mapFla.get(key).mem;
					}
					
					if(!f || tCpu > sCpu || tMem > sMem){
						continue;
					}
					int fx0 = 0;
					if("CPU".equals(flag)){
						fx0 = tCpu;
					}
					else{
						fx0 = tMem;
					}
						
					//是否采用新解
					int cfx = fx0 - fx;
					if(cfx >= 0){
						tmap = ttmap;
						fx = fx0;
					}
					else{
						if(Math.exp(cfx/t) > Math.random()){
							tmap = ttmap;
							fx = fx0;
						}
					}
				}
				t = t * delta;
			}
				
			//模拟退火
			server.map = tmap;
			for(String key : tmap.keySet()){
				map.put(key, map.get(key) - tmap.get(key));
				tSum -= tmap.get(key);
			}
				
			servers.add(server);
			sNum++;
		}
		return servers;
	}


	//方法四：模拟退火+首次适应
	public static List<Server> mnth2(Map<String, Integer> map, Map<String, Flavour> mapFla, int sCpu, int sMem, String flag){
		List<Server> servers = new ArrayList<>();
		
		List<Flavour> flavours = new ArrayList<>();//保存所有flavor
		int n = 0;
		for(String key : map.keySet()){
			for(int i=0; i<map.get(key); i++){
				flavours.add(mapFla.get(key));
				n++;
			}
		}
		//###################
		//System.out.println(n);
		double t = 100.0;//初始温度
		double tMin = 1e-4;//终止温度
		int k = 100;//内循环迭代次数
		double delta = 0.9; //温度下降率
		
		servers = ff(flavours, sCpu, sMem);
		int fx = servers.size();
		List<Server> servers0 = new ArrayList<>();
		
		while(t > tMin){
			for(int i=0; i<k; i++){
				//生成两个不同的随机数,并生成新解
				int a = new Random().nextInt(n);
				int b = 0;
				do{
					b = new Random().nextInt(n);
				}while(a == b);
				Flavour temp = flavours.get(a);
				flavours.set(a, flavours.get(b));
				flavours.set(b, temp);
				
				servers0 = ff(flavours, sCpu, sMem);
				int fx0 = servers0.size();
				
				//是否采用新解
				int cfx = fx0 - fx;
				if(cfx <= 0){
					servers = servers0;
					fx = fx0;
				}
				else{
					if(Math.exp(-cfx/t) > Math.random()){
						servers = servers0;
						fx = fx0;
					}
				}
			}
			t = t * delta;
		}
		
		return servers;
	}
	
	//方法五：ff
	public static List<Server> ff(List<Flavour> flavours, int sCpu, int sMem){
		List<Server> servers = new ArrayList<>();
		int sNum = 0;
		List<Flavour> newFlavours = new ArrayList<>(flavours);
		
		while(flavours.size() > 0){
			if(sNum == 0){
				Server server = new Server(sNum+1, sCpu, sMem);
				servers.add(server);
				sNum++;		
			}
			
			for(Flavour fla : flavours){
				boolean find = false;
				for(Server ser : servers){
					if((fla.cpu < ser.cpu)&&(fla.mem < ser.mem)){
						ser.cpu -= fla.cpu;
						ser.mem -= fla.mem;
						ser.map.put(fla.name, ser.map.merge(fla.name, 1, Integer::sum));
						
						find = true;
						newFlavours.remove(fla);
						break;
					}
				}
				if(!find){
					Server server = new Server(sNum+1, sCpu, sMem);
					servers.add(server);
					sNum++;
					
				}
				
				flavours = new ArrayList<>(newFlavours);
			}
		}

		return servers;
	}
	
	

}
