package com.sky.testlinkconvert;

import java.io.*;
import java.util.*;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import org.dom4j.*;
import org.dom4j.io.*;
import java.util.regex.*;

/**
 * 将testlink中的xml文件转换成excel（测试用例）
 * 
 * 以测试套件形式导出的xml，转换后的格式：模块、子模块、用例名称、预置条件、操作步骤、预期结果、用例等级、摘要 
 * 导出所有间接测试套件作为模块名（套件与套件间用"/"连接），测试用例直接所属套件名作为子模块名
 * 
 * 以测试用例形式导出的xml，转换后的格式：用例名称、预置条件、操作步骤、预期结果、用例等级、摘要
 * 
 * @author Rachel.Luo
 * */
public class XmlToExcel {
	private static List<String> title = new ArrayList<String>();
	private static WritableWorkbook wwb;
	private static WritableSheet ws;
	private static Element root;
	private static String module_name = null; // 每个用例的模块
	private static String sub_module_name = null; // 每个用例的子模块
	private static Element ppelement = null;   //用例的直属父节点的父节点
	
	public static void transferXMLToExcel(String oldfilename) {
		long time = System.currentTimeMillis();
		File f = new File(oldfilename);
		String newfilename = getExcelName(oldfilename, time);
		System.out.println("newfilename:" + newfilename);
		System.out.println("converting,please wait...");
		
		//初始化static属性
		module_name = null; 
		sub_module_name = null; 
		ppelement = null;   
		
		//添加固定会导出的列标题
		title.add("用例名称");
		title.add("总结条件");
		title.add("操作步骤");
		title.add("预期结果");

		SAXReader reader = new SAXReader();
		Document doc;
		try {
			doc = reader.read(f);
			root = doc.getRootElement();

			if (root.getName().equals("testcases")) {// 当根节点是testcases，不输出模块和子模块
				forEachElement(root, newfilename);

			} else if (root.getName().equals("testsuite")) {// 当根节点是testsuites，需要输出模块和子模块
				title.add(0, "模块");
				
				forEachElement(root,newfilename);
			}
			title.clear();
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 遍历根节点下的所有节点，寻找testsuite和testcase节点；
	 * 对于testcase节点，读取用例的"用例名称","用例等级","预置条件","操作步骤","预期输出","摘要"等值；并写入excel表格中。
	 * 对于testsuite节点，继续遍历寻找testsuite和testcase节点，直到找到testcase节点才结束。
	 * 
	 * testcase的非直属套件的name全部以"/"连接起来，导出作为模块名
	 * 用例直接所属testsuite节点的name属性，导出作为子模块名
	 * 
	 * @throws Exception
	 * */
	private static void forEachElement(Element element, String newfilename)
			throws Exception {
		List<String> testcase=null;
		
		for (Iterator it = element.elementIterator(); it.hasNext();) {
			Element subelement = (Element) it.next();
			String text = subelement.getName();
			
			//若是testsuite节点，递归遍历每一个节点，直至testcase节点
			if (text.equals("testsuite")) { 
				forEachElement(subelement,newfilename);
				
			}else if (text.equals("testcase")) {
				//否则要输出模块和子模块  （测试用例不可能建在项目下面）
					/**
					 * element为测试用例的直属父节点；
					 * 若测试用例的直属父节点为根节点（套件名），模块为根节点，子模块为""；
					 * 若测试用例的直属父节点为根节点（项目名）的子节点，模块为直属父节点名，子模块为""；
					 * 其他情况，直属父节点名为子模块；
					 * */
					//模块和子模块的特殊处理

					String rootname = root.attributeValue("name");
					Element sup_element = subelement.getParent();     //用例的直属父节点的父节点
//					if(element.equals(root)&& !rootname.equals("")){
//						module_name = rootname;
//						sub_module_name="";
//
//					}else if(sup_element.equals(root)&&sup_element.attributeValue("name").equals("")){
//						if(ppelement == null){ //初次赋值
//							ppelement = sup_element;
//						}
//						module_name = element.attributeValue("name");
//						sub_module_name="";
//					}else{//模块和子模块的普通处理
//						sub_module_name=element.attributeValue("name"); //实时获取每个用例的直属父节点名
//
//						/**
//						 * 保存或更新上一个testcase的直属父节点的父节点，
//						 * 第一个testcase的模块名，根据直属父节点的父节点逆向追溯到根节点，保存其模块名；
//						 * 当前testcase的直属父节点的父节点与之相同，说明模块相同，模块不改变；
//						 * 否则逆向追溯到根节点，更新模块名；
//						 * */
//						if(ppelement == null){ //初次赋值
//							ppelement = sup_element;
//						}
//						if(module_name == null){ //初次赋值
//							module_name =getModuleName(sup_element);
//						}
//						//当前testcase的直属父节点的父节点与之前的不相同，更新保存ppelement和module_name
//						if(!sup_element.equals(ppelement)){
//							module_name =getModuleName(sup_element);
//							ppelement = sup_element;
//						}

//					}

				    module_name=sup_element.attributeValue("name");
					testcase = parseTestCaseTag(subelement,module_name,sub_module_name);


				//将用例写入Excel中
				writeExcelByLine(newfilename, testcase);
			}
		}
	}

	private static String getModuleName(Element sup_element){
		StringBuffer temp = new StringBuffer();
		List<String> suite_names=new ArrayList<String>();
		//只要ppelement不是根节点，就继续寻找父节点
		Element supe = sup_element;
		while(!supe.equals(root)){
			suite_names.add(supe.attributeValue("name"));
			supe = supe.getParent();
		}
		//当supe为根节点时,name不等于""，才保存
		if(!supe.attributeValue("name").equals("")){
			suite_names.add(supe.attributeValue("name"));
		}
		Collections.reverse(suite_names);
		for(int i=0;i<suite_names.size();i++){
			temp.append(suite_names.get(i));
			if(i!=suite_names.size()-1){
				temp.append("/");
			}
		}
		return temp.toString();
	} 
	
	// 从<testcase>标签中解析用例各字段信息，并保存到List中 , 注： subelement为<testcase>
	private static List<String> parseTestCaseTag(Element subelement,
			String module_name, String sub_module_name) {
		List<String> testcase = new ArrayList<String>();
		// 添加模块和子模块
		if (module_name != null) {
			testcase.add(module_name);

		}

		// 取得节点testcase的name属性的值.
		String casename = subelement.attributeValue("name");
//		String preconditions = subelement.elementText("preconditions");
		// 保存用例名称和预置条件
		testcase.add(replace1(casename));
		String summary = subelement.elementText("summary");
		testcase.add(replace(summary));
		String steps = subelement.elementText("steps");
		testcase.add(replace(steps));
		String expectedresults = subelement.elementText("expectedresults");
		testcase.add(replace(expectedresults));
		// 获取操作步骤和预期输出，并保存
//		Element steps = subelement.element("steps");
//		if (steps != null) {
//			List<Element> step = steps.elements("li");
//			StringBuffer actions = new StringBuffer();
//			StringBuffer expectedresults = new StringBuffer();
//			for (int i = 0; i < step.size(); i++) {
////				if (i == step.size() - 1) {
//					actions.append(step.get(i).elementText("actions"));
//					expectedresults.append(step.get(i).elementText(
//							"expectedresults"));
////				} else {
////					actions.append(step.get(i).elementText("actions") + "\n");
////					expectedresults.append(step.get(i).elementText(
////							"expectedresults")
////							+ "\n");
////				}
//			}
//			if (step.size() != 0) {
//				testcase.add(replace(actions.toString()));
//				testcase.add(replace(expectedresults.toString()));
//			} else {
//				testcase.add("");
//				testcase.add("");
//			}
//		} else {
//			testcase.add("");
//			testcase.add("");
//		}

		// 取得节点testcase的importance节点的文本. 即获取用例等级，并保存
//		String importance = subelement.elementText("importance");
//		if(importance==null||importance==""){
//			importance = "低";
//		}else if (importance.equals("1")) {
//			importance = "低";
//		} else if (importance.equals("2")) {
//			importance = "中";
//		} else if (importance.equals("3")) {
//			importance = "高";
//		}
//		testcase.add(replace1(importance));
//		// 获取摘要，并保存


		return testcase;
	}

	// 一行一行的将测试用例写入excel表中
	private static void writeExcelByLine(String newfilename,
			List<String> testcase) throws Exception {
		File file = new File(newfilename);

		if (!file.exists()) { // 文件不存在时，创建文件并写入标题和第一个用例
			try {
				// 以fileName为文件名来创建一个新的Workbook对象
				wwb = Workbook.createWorkbook(file);
				// 创建工作表
				ws = wwb.createSheet("TestCase", 0);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			for (int i = 0; i < title.size(); i++) {
				Label label0 = new Label(i, 0, title.get(i));
				Label label = new Label(i, 1, testcase.get(i));
				ws.addCell(label0);
				ws.addCell(label);
			}
			// 写进文档
			wwb.write();
			wwb.close();

		} else {// 文件存在时，接着写文件
			Workbook twb = Workbook.getWorkbook(file);

			// 根据已存在的excel，创建一个Workbook对象
			wwb = Workbook.createWorkbook(file, twb);
			// 获取第一个工作表
			ws = wwb.getSheet(0);
			// 获取行数
			int num = ws.getRows();
			for (int i = 0; i < title.size(); i++) {
				Label label = new Label(i, num, testcase.get(i));
				ws.addCell(label);
			}
			// 写进文档
			wwb.write();
			wwb.close();
			twb.close();
		}
	}

	// 根据老的xml文件名 获取新的excel文件名
	private static String getExcelName(String oldfilename, long time) {
		String newfilename = "";
		String[] temp = oldfilename.split("\\\\");
		String prename = temp[temp.length - 1].substring(0,
				temp[temp.length - 1].length() - 4);
		newfilename = oldfilename.substring(0, oldfilename.length()
				- temp[temp.length - 1].length())
				+ "Excel_" + prename + "_" + time + ".xls";

		return newfilename;
	}

	//去掉字符串中的特殊字符
	private static String replace(String oldStr) {
		if (oldStr != null) {

			String newStr = "";
//			newStr = oldStr.replaceAll("</br>","\r\n");   // 替换</br>为\r\n
//			newStr = newStr.replaceAll("</p>","\r\n");   // 替换</p>为\r\n
//			newStr = newStr.replaceAll("<([^>]*)>", ""); // 替换掉html标签
//			newStr = newStr.replaceAll("&\\w*;", ""); // 替换以&开头及;结束的符号

			Pattern pat = Pattern.compile("\\<li\\>(.*)\\</li\\>");

			Matcher ma =  pat.matcher(oldStr);

			while(ma.find()){
				newStr +=ma.group(1)+"\r\n";

			}
			newStr = newStr.replaceAll("&nbsp;", "");
			return newStr;
		} else {
			return "";
		}
	}
	private static String replace1(String oldStr) {
		if (oldStr != null) {

			String newStr = "";
			newStr = oldStr.replaceAll("</br>","\r\n");   // 替换</br>为\r\n
			newStr = newStr.replaceAll("</p>","\r\n");   // 替换</p>为\r\n
			newStr = newStr.replaceAll("<([^>]*)>", ""); // 替换掉html标签
			newStr = newStr.replaceAll("&\\w*;", ""); // 替换以&开头及;结束的符号


			return newStr;
		} else {
			return "";
		}
	}

}
