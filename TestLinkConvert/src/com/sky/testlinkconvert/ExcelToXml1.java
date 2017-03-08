package com.sky.testlinkconvert;

import org.apache.poi.POIXMLException;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 将写有用例的excel文件转成xml文件,均以套件形式导入，支持2类模板:
 * 模板一（固定4列）：
 *  [测试序号] 用例名称   预置条件    操作步骤     预期结果  + [随机额外列（含用例等级）] 
 * 模板二（固定6列）：
 *  [测试序号] 模块   子模块    用例名称   预置条件    操作步骤     预期结果 + [随机额外列（含用例等级）] 
 * 注意：
 * 以上固定的列，要求必须有，且顺序必须跟模板相同，
 * []括起的列可以有也可以没有，如果有的话，"测试序号"和"用例等级"名称必须如此，其他额外列名称可自定义；
 * 测试序号若有，必须位于第一列，其他额外列放在"预期结果"后，顺序随意；
 * []括起的列，除了用例等级，其他的信息都将导入到testlink中用例的"摘要"信息中。
 * 
 * @author Rachel.Luo
 * */
public class ExcelToXml1 {
	private static int internalid = 1000001;
	private static int suite_node = 1;
	private static List<String> modules = new ArrayList<String>();  //保存：模块名+"/"+子模块名，重复只保存一次
	//excel模板支持4个/6个固定列，及多个随机列；extracols不为empty，信息计入摘要中
	private static List<String> titles = new ArrayList<String>(); //放所有列名称
	private static List<String> extracols = new ArrayList<String>(); //放额外的标题名称
	private static int yq_index;   //预期结果列的下标
	private static int m_index;    //模块列的下标
	private static int sm_index;   //子模块列的下标
	
	public static void transferExcelToXml(String oldfilename){
		long time = System.currentTimeMillis();
		String newfilename = getXmlName(oldfilename,time);
		System.out.println("newfilename:"+newfilename);
		System.out.println("converting,please wait...");
		
		//初始化static属性值
		internalid = 1000001;
		suite_node = 1;
		modules.clear();
		titles.clear();
		extracols.clear();
		
		// 默认创建2007版本的Excel文件对象
        XSSFWorkbook xswb = null;
        //出现异常时，创建2003版本的Excel文件对象
        HSSFWorkbook hswb = null;
        try {  
            xswb = new XSSFWorkbook(new FileInputStream(oldfilename));
            // 创建对工作表的引用
            XSSFSheet xssheet = xswb.getSheetAt(0);
            
			List<String> caseatrs;
			String tempfile=""; //临时文件的定义
			
			//获取行号
            int num = xssheet.getLastRowNum(); 
			//获取标题行的列数
			XSSFRow xsrow0 = xssheet.getRow(0); 
			int col = xsrow0.getLastCellNum();
			//将列标题保存起来
			for(int r=0;r<col;r++){
				titles.add(xsrow0.getCell(r).getStringCellValue());
			}
			yq_index = titles.indexOf("预期结果");
			//若预期结果不是最后一列，则有额外列，保存额外列名称
			for(int cn=yq_index+1;cn<col;cn++){
				extracols.add(xsrow0.getCell(cn).getStringCellValue());
			}
			
			//若存在模块列和子模块列，记录下这两列的下标
			if(yq_index-4>=0 && yq_index-5>=0){
				m_index = yq_index-5;
				sm_index = yq_index-4;
			}
			caseatrs = new ArrayList<String>();
			//获取用例各列信息
			for (int i=1; i<=num; i++) {

				
				for(int j=0;j<col;j++){ 
					XSSFCell temp =xssheet.getRow(i).getCell(j);
					if(temp!=null ||!temp.equals("")){
						if(temp.getCellType()==temp.CELL_TYPE_NUMERIC){
							DecimalFormat df = new DecimalFormat("###0");
							caseatrs.add(replaceCellAngleBrackets(df.format(temp.getNumericCellValue())));
						}else{
							caseatrs.add(replaceCellAngleBrackets(temp.getStringCellValue()));
						}
					}else{
						caseatrs.add("");
					}
				}
				
				//先写到临时xml文件中,创建临时文件所在目录，再完成临时文件的赋值

			}
			File temp = new File("c:");
			if(temp.exists()){//有c盘
				if(!new File("c:\\temp").exists()){
					new File("c:\\temp").mkdirs();
				}
				tempfile="c:\\temp\\tempfile_"+time+".xml";
			}else{//无c盘
				String path = oldfilename.split("\\\\")[0];
				if(!new File(path+"\\temp").exists()){
					new File(path+"\\temp").mkdirs();
				}
				tempfile=path+"\\temp\\tempfile_"+time+".xml";
			}

			int result = writeTestcaseToXml(tempfile, caseatrs);
			if(result==-1){
				return ;
			}
			//将临时文件的内容重新写到最终的xml文件中
			replaceESC(tempfile,newfilename);
            
        } catch (FileNotFoundException e) {  
        	e.printStackTrace(); 
        } catch (POIXMLException e) {  
			try {
				hswb = new HSSFWorkbook(new FileInputStream(oldfilename));
				HSSFSheet hssheet = hswb.getSheetAt(0);  
				
				List<String> caseatrs;
				String tempfile="";  //临时文件的定义
				
				//获取行号
	            int num = hssheet.getLastRowNum(); 
				//获取标题行的列数
				HSSFRow hsrow0 = hssheet.getRow(0); 
				int col = hsrow0.getLastCellNum();
				//将列标题保存起来
				for(int r=0;r<col;r++){
					titles.add(hsrow0.getCell(r).getStringCellValue());
				}
				yq_index = titles.indexOf("预期结果");
				//若预期结果不是最后一列，则有额外列，保存额外列名称
				for(int cn=yq_index+1;cn<col;cn++){
					extracols.add(hsrow0.getCell(cn).getStringCellValue());
				}
				
				//若存在模块列和子模块列，记录下这两列的下标
				if(yq_index-4>=0 && yq_index-5>=0){
					m_index = yq_index-5;
					sm_index = yq_index-4;
				}
				caseatrs = new ArrayList<String>();
				//获取用例的各列信息
				for (int i=1; i<=num; i++) {

					
					for(int j=0;j<col;j++){ 
						HSSFCell temp =hssheet.getRow(i).getCell(j);
						if(temp!=null ||!temp.equals("")){
							if(temp.getCellType()==temp.CELL_TYPE_NUMERIC){
								DecimalFormat df = new DecimalFormat("###0");
								caseatrs.add(replaceCellAngleBrackets(df.format(temp.getNumericCellValue())));
							}else{
								caseatrs.add(replaceCellAngleBrackets(temp.getStringCellValue()));
							}
						}else{
							caseatrs.add("");
						}
					}
					
					//先写到临时xml文件中,创建临时文件所在目录，再完成临时文件的赋值

				}
				File temp = new File("c:");
				if(temp.exists()){//有c盘
					if(!new File("c:\\temp").exists()){
						new File("c:\\temp").mkdirs();
					}
					tempfile="c:\\temp\\tempfile_"+time+".xml";
				}else{//无c盘
					String path = oldfilename.split("\\\\")[0];
					if(!new File(path+"\\temp").exists()){
						new File(path+"\\temp").mkdirs();
					}
					tempfile=path+"\\temp\\tempfile_"+time+".xml";
				}

				int result = writeTestcaseToXml(tempfile, caseatrs);
				if(result==-1){
					return ;
				}
				//将临时文件的内容重新写到最终的xml文件中
				replaceESC(tempfile,newfilename);
				
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (NullPointerException e1) {
				// TODO Auto-generated catch block
				System.out.println("In 2003 excel,TestCase and TestCase between can't have empty row.");
				System.out.println("OR some columns or some rows are not in border.");
				e1.printStackTrace();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        } catch (IOException e) {  
        	e.printStackTrace();   
        } catch (NullPointerException e1) {
			// TODO Auto-generated catch block
			System.out.println("In 2007 excel,TestCase and TestCase between can't have empty row.");
			System.out.println("OR some columns or some rows are not in border.");
			e1.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * 一行一行的将每个测试用例写入xml中
	 * 
	 * 根据模块和子模块名称，添加具体的测试套件，模块和子模块各对应一个
	 * 模块和子模块为空时，不添加对应的测试套件
	 * */
	private static int writeTestcaseToXml(String newfilename,
			List<String> caseatrs) throws Exception {
//		try {
			XMLWriter writer = null;// 声明写XML的对象
			SAXReader reader = new SAXReader();
			Document document = null;
			
			OutputFormat format = OutputFormat.createPrettyPrint();
			format.setEncoding("UTF-8");// 设置XML文件的编码格式
			File file = new File(newfilename);
			document = reader.read(file);
			Element root = document.getRootElement();
			Element sub_testsuite = root.addElement("testsuite");
			sub_testsuite.addAttribute("name", "");
			Element sub_node = sub_testsuite.addElement("node_order");
			sub_node.setText("<![CDATA[]]>");
			Element sub_details = sub_testsuite.addElement("details");
			sub_details.setText("<![CDATA[]]>");

//			if (file.exists()) {   //读取存在的testcase.xml文件，并追加测试用例
//				document = reader.read(file);  //读取XML文件
//				Element root = document.getRootElement();   //得到根节点
//
//				//无模块和子模块列，直接将用例添加到根节点下
//				if(yq_index-4<0){
//					addTestCase(root, caseatrs);
//
//				}else{//有模块和子模块列
//					//模块和子模块列有值
//					Element sub_testsuite = root.addElement("testsuite");
//					sub_testsuite.addAttribute("name", caseatrs.get(sm_index));
//					Element sub_node = sub_testsuite.addElement("node_order");
//					sub_node.setText("<![CDATA["+suite_node+"]]>");
//					Element sub_details = sub_testsuite.addElement("details");
//					sub_details.setText("<![CDATA[]]>");
//					if(!caseatrs.get(m_index).equals("")&& !caseatrs.get(sm_index).equals("")){
//						String module = caseatrs.get(m_index)+"/"+caseatrs.get(sm_index);
//
//						//模块名和子模块名都已有对应的测试套件
//						if(modules.contains(module)){
//							//获取已有测试套件，并添加测试用例
//							Element element = getTestsuiteByModule(root, module);
//							//添加测试用例
//							addTestCase(element, caseatrs);
//						}else if(modules.contains(caseatrs.get(m_index))){ //模块名已有对应的测试套件
//							//获取已有父测试套件，新建子测试套件，再添加测试用例
//							Element element = getTestsuiteByModule(root, caseatrs.get(m_index));
//							//新建子测试套件
//							Element sub_testsuite = element.addElement("testsuite");
//							sub_testsuite.addAttribute("name", caseatrs.get(sm_index));
//							Element sub_node = sub_testsuite.addElement("node_order");
//							sub_node.setText("<![CDATA["+suite_node+"]]>");
//							Element sub_details = sub_testsuite.addElement("details");
//							sub_details.setText("<![CDATA[]]>");
//							suite_node++;
//							//在子测试套件下添加测试用例
//							addTestCase(sub_testsuite, caseatrs);
//
//							//保存新建模块
//							modules.add(module);
//						}else {
//							//模块拆开，看是否含已建的套件
//							String[] suites = module.split("/");
//							String part_module="";
//							for(String m:modules){
//								StringBuffer sbstr = new StringBuffer();
//								for(String str:suites){
//									if(m.contains(str)){
//										sbstr.append(str);
//										sbstr.append("/");
//									}else{
//										//一旦找不到,保存最长的相同部分，并跳出里层循环
//										if(!sbstr.toString().equals("")){
//											String tempStr = sbstr.toString().substring(0, sbstr.toString().length()-1);
//											if(tempStr.length()> part_module.length()){
//												part_module = tempStr;
//											}
//										}
//										break;
//									}
//								}
//							}
//							if(!part_module.equals("")){
//								//获取已存在的套件，在其下添加未创建的套件，再添加测试用例
//								Element element = getTestsuiteByModule(root, part_module);
//								String[] newsuites = module.substring(module.indexOf(part_module)+part_module.length()+1).split("/");
//								Element sub_testsuite = null;
//								for(int i =0;i<newsuites.length;i++){
//									sub_testsuite = element.addElement("testsuite");
//									sub_testsuite.addAttribute("name", newsuites[i]);
//									Element sub_node = sub_testsuite.addElement("node_order");
//									sub_node.setText("<![CDATA["+suite_node+"]]>");
//									Element sub_details = sub_testsuite.addElement("details");
//									sub_details.setText("<![CDATA[]]>");
//									suite_node++;
//									element = sub_testsuite;
//								}
//								//在子测试套件下添加测试用例
//								addTestCase(sub_testsuite, caseatrs);
//								//保存新建模块
//								modules.add(module);
//
//							}else{//遍历所有已保存模块，进行比较，发现当前模块和子模块都不存在，需要新建
//								//先建父测试套件
//								Element testsuite = createTestsuitesByModule(root,caseatrs);
//
//								//再建子测试套件
//								Element sub_testsuite = testsuite.addElement("testsuite");
//								sub_testsuite.addAttribute("name", caseatrs.get(sm_index));
//								Element sub_node = sub_testsuite.addElement("node_order");
//								sub_node.setText("<![CDATA["+suite_node+"]]>");
//								Element sub_details = sub_testsuite.addElement("details");
//								sub_details.setText("<![CDATA[]]>");
//								suite_node++;
//								//在子测试套件下添加测试用例
//								addTestCase(sub_testsuite, caseatrs);
//								//保存新建模块
//								modules.add(module);
//							}
//						}
//
//					//模块列有值,子模块列无值，只建父测试套件
//					}else if(!caseatrs.get(m_index).equals("")&& caseatrs.get(sm_index).equals("")){
//
//						//模块已有对应的测试套件
//						if(modules.contains(caseatrs.get(m_index))){
//							//获取已有测试套件，并添加测试用例
//							Element element = getTestsuiteByModule(root,caseatrs.get(m_index));
//							//添加测试用例
//							addTestCase(element, caseatrs);
//						}else {
//							//模块拆开，看是否含已建的套件
//							String[] suites = caseatrs.get(m_index).split("/");
//							String part_module="";
//							for(String m:modules){
//								StringBuffer sbstr = new StringBuffer();
//								for(String str:suites){
//									if(m.contains(str)){
//										sbstr.append(str);
//										sbstr.append("/");
//									}else{
//										//一旦找不到,保存最长的相同部分，并跳出里层循环
//										if(!sbstr.toString().equals("")){
//											String tempStr = sbstr.toString().substring(0, sbstr.toString().length()-1);
//											if(tempStr.length()>part_module.length()){
//												part_module = tempStr;
//											}
//										}
//										break;
//									}
//								}
//							}
//							if(!part_module.equals("")){
//								//获取已存在的套件，在其下添加未创建的套件，再添加测试用例
//								Element element = getTestsuiteByModule(root, part_module);
//								String[] newsuites = caseatrs.get(m_index).substring(caseatrs.get(m_index).indexOf(part_module)+part_module.length()+1).split("/");
//								Element sub_testsuite = null;
//								for(int i =0;i<newsuites.length;i++){
//									sub_testsuite = element.addElement("testsuite");
//									sub_testsuite.addAttribute("name", newsuites[i]);
//									Element sub_node = sub_testsuite.addElement("node_order");
//									sub_node.setText("<![CDATA["+suite_node+"]]>");
//									Element sub_details = sub_testsuite.addElement("details");
//									sub_details.setText("<![CDATA[]]>");
//									suite_node++;
//									element = sub_testsuite;
//								}
//								//在子测试套件下添加测试用例
//								addTestCase(sub_testsuite, caseatrs);
//								//保存新建模块
//								modules.add(caseatrs.get(m_index));
//
//							}else{//遍历所有已保存模块，进行比较，发现当前模块不存在，需要新建
//								//建父测试套件，直接添加测试用例
//								Element testsuite = createTestsuitesByModule(root,caseatrs);
//
//								//添加测试用例
//								addTestCase(testsuite, caseatrs);
//								//保存新建模块
//								modules.add(caseatrs.get(m_index));
//							}
//						}
//					}else if(caseatrs.get(m_index).equals("")&& caseatrs.get(sm_index).equals("")){
//						//模块和子模块列均无值，不建测试套件，直接添加测试用例
//						addTestCase(root, caseatrs);
//
//					}else{
//						System.out.println("converting Fail! Caused by:module name is empty when child module name is not empty!");
//						return -1;
//					}
//				}
//			} else {
//				//新建testcase.xml文件
//				document = DocumentHelper.createDocument();
//				//建根节点
//				Element root = document.addElement("testsuite");
//
//				//无模块和子模块列，直接将用例添加到根节点下
//				if(yq_index-4<0){
//					addTestCase(root, caseatrs);
//
//				}else{//有模块和子模块列
//					if(!caseatrs.get(m_index).equals("")&& !caseatrs.get(sm_index).equals("")){
//						String module = caseatrs.get(m_index)+"/"+caseatrs.get(sm_index);
//
//						//模块和子模块有值，先建父测试套件
//						Element testsuite = createTestsuitesByModule(root,caseatrs);
//						//再建子测试套件
//						Element sub_testsuite = testsuite.addElement("testsuite");
//						sub_testsuite.addAttribute("name", caseatrs.get(sm_index));
//						Element sub_node = sub_testsuite.addElement("node_order");
//						sub_node.setText("<![CDATA["+suite_node+"]]>");
//						Element sub_details = sub_testsuite.addElement("details");
//						sub_details.setText("<![CDATA[]]>");
//						suite_node++;
//						//在子测试套件下添加测试用例
//						addTestCase(sub_testsuite, caseatrs);
//
//						//保存新建模块
//						modules.add(module);
//					}else if(!caseatrs.get(m_index).equals("")&& caseatrs.get(sm_index).equals("")){
//						//模块有值,子模块无值，只建父测试套件
//						Element testsuite = createTestsuitesByModule(root,caseatrs);
//						//添加测试用例
//						addTestCase(testsuite, caseatrs);
//
//						//保存新建模块
//						modules.add(caseatrs.get(m_index));
//					}else if(caseatrs.get(m_index).equals("")&& caseatrs.get(sm_index).equals("")){
//						//模块和子模块均无值，不建测试套件，直接添加测试用例
//						addTestCase(root, caseatrs);
//
//					}else{
//						System.out.println("converting Fail! Caused by:module name is empty when child module name is not empty!");
//						return -1;
//					}
//				}
//			}
//			writer = new XMLWriter(new FileWriter(newfilename), format);
//			writer.write(document);
//			writer.close();
//			internalid++;
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		return 0;
	}

	//根据模块名创建外层测试套件
	private static Element createTestsuitesByModule(Element root,List<String> caseatrs){
		//建外层测试套件
		Element child_element = null;
		if(!caseatrs.get(m_index).contains("/")){
			Element testsuite = root.addElement("testsuite");
			testsuite.addAttribute("name", caseatrs.get(m_index));
			Element node = testsuite.addElement("node_order");
			node.setText("<![CDATA["+suite_node+"]]>");
			Element details = testsuite.addElement("details");
			details.setText("<![CDATA[]]>");
			suite_node++;
			child_element = testsuite;
		}else{
			String[] suite_names = caseatrs.get(m_index).split("/");
			Element temp = root;
			for(String suite_name:suite_names){
				Element testsuite = temp.addElement("testsuite");
				testsuite.addAttribute("name", suite_name);
				Element node = testsuite.addElement("node_order");
				node.setText("<![CDATA["+suite_node+"]]>");
				Element details = testsuite.addElement("details");
				details.setText("<![CDATA[]]>");
				suite_node++;
				temp = testsuite;
			}
			child_element = temp;
		}

		return child_element;
	}

	//假如sub_module改变:传递module；否则，传递module+"/"+sub_module
	private static Element getTestsuiteByModule(Element root,String module){
		Element testSuite = null;
		String[] suites = module.split("/");
		int num = 0;
		Iterator<Element>  it = root.elementIterator("testsuite");

		while(it.hasNext()){
			Element element = it.next();
			if(element.attributeValue("name").equals(suites[num])){
				num++;
				if(num==suites.length){
					testSuite = element;
					break;
				}else{
					it=element.elementIterator("testsuite");
				}
			}
		}
		return testSuite;
	}

	private static void addTestCase(Element sup_element,List<String> caseatrs){
		//添加一个testcase
		Element testcase = sup_element.addElement("testcase");
		testcase.addAttribute("internalid", internalid + "");
		testcase.addAttribute("name", caseatrs.get(yq_index-3));

		//将测试序号及额外列（除开用例等级）信息导入到summary（摘要）中
		Element summary = testcase.addElement("summary");
		StringBuffer sumStr = new StringBuffer();
		if(titles.indexOf("测试序号")==0){
			sumStr.append("测试序号："+caseatrs.get(0));
		}
		for(String col:extracols){
			if(col.equals("摘要")){
				sumStr.append("</br>");
				sumStr.append(caseatrs.get(extracols.indexOf(col)+yq_index+1).replaceAll("\n", "</br>"));
			}else if(!col.equals("用例等级")){
				sumStr.append("</br>");
				sumStr.append(col+"："+caseatrs.get(extracols.indexOf(col)+yq_index+1));
			}
		}
		summary.setText("<![CDATA["+sumStr.toString()+"]]>");

		Element preconditions = testcase.addElement("preconditions");
		preconditions.setText("<![CDATA[" + caseatrs.get(yq_index-2).replaceAll("\n", "</br>")+"]]>");
		Element execution_type = testcase.addElement("execution_type");
		execution_type.setText("<![CDATA[1]]>");

		//额外列中，如果有用例等级，取对应的用例等级导入；如果无用例等级，默认用例等级为"2"
		Element importance = testcase.addElement("importance");
		int index = extracols.indexOf("用例等级");
		if(index!=-1){
			index =(yq_index+1)+index;
			if (caseatrs.get(index).equals("低")||caseatrs.get(index).equals("1")) {
				importance.setText("<![CDATA[" + 1 + "]]>");
			} else if (caseatrs.get(index).equals("中")||caseatrs.get(index).equals("2")) {
				importance.setText("<![CDATA[" + 2 + "]]>");
			} else if (caseatrs.get(index).equals("高")||caseatrs.get(index).equals("3")) {
				importance.setText("<![CDATA[" + 3 + "]]>");
			}
		}else{
			importance.setText("<![CDATA[" + 2 + "]]>");
		}
		Element steps = testcase.addElement("steps");
		Element step = steps.addElement("step");
		Element step_number = step.addElement("step_number");
		step_number.setText("<![CDATA[1]]>");
		Element actions = step.addElement("actions");
		actions.setText("<![CDATA[" + caseatrs.get(yq_index-1).replaceAll("\n", "</br>")+"]]>");
		Element expectedresults = step.addElement("expectedresults");
		expectedresults.setText("<![CDATA["+caseatrs.get(yq_index).replaceAll("\n", "</br>")+"]]>");
	}

	// 根据老的文件名 获取新的文件名
	private static String getXmlName(String oldfilename,long time) {
		String newfilename = "";
		String[] temp = oldfilename.split("\\\\");
		String name = temp[temp.length - 1].split("\\.")[0]; //文件名前缀
		name = name.replaceAll("[0-9]*", "");
		if(name.endsWith("_")==true){
			newfilename = oldfilename.substring(0, oldfilename.length()
					- temp[temp.length - 1].length())
					+ "TestCase_"+name+time+".xml";
		}else{
			newfilename = oldfilename.substring(0, oldfilename.length()
					- temp[temp.length - 1].length())
					+ "TestCase_"+name+"_"+time+".xml";
		}
		return newfilename;
	}

	//简单替换每列内容中的<>符号为小于和大于；最好要求用户不要使用尖括号，否则会替换成大于小于
	private static String replaceCellAngleBrackets(String cellStr) throws Exception{
		String result="";
		if(cellStr.contains("<") && cellStr.contains(">")){
			result=cellStr.replaceAll("<", "小于");
			result=result.replaceAll(">", "大于");
		}else if(cellStr.contains("<")){
			result=cellStr.replaceAll("<", "小于");
		}else if(cellStr.contains(">")){
			result=cellStr.replaceAll(">", "大于");
		}else{
			result = cellStr;
		}
		return result;
	}

	//替换xml文件中的转义字符
	private static void replaceESC(String tempfile,String newfilename) throws Exception{
		File file = new File(tempfile);
		FileInputStream fis = new FileInputStream(tempfile);
		InputStreamReader isr = new InputStreamReader(fis);
		BufferedReader br = new BufferedReader(isr);

		FileOutputStream fos = new FileOutputStream(newfilename,true);
		OutputStreamWriter osw = new OutputStreamWriter(fos);
		BufferedWriter bw = new BufferedWriter(osw);

		//一行一行的读，一行一行的写
		String line;
		while((line=br.readLine())!=null){
			String tempstr=line.replaceAll("&lt;", "<");
			tempstr=tempstr.replaceAll("&gt;", ">");
			bw.write((tempstr+"\n"));
		}
		br.close();
		isr.close();
		fis.close();
		bw.close();
		osw.close();
		fos.close();

		//删除临时文件
		file.delete();
	}

}
