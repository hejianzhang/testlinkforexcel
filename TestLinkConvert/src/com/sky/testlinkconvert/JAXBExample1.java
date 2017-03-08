package com.sky.testlinkconvert;

/**
 * Created by Administrator on 2017-03-08.
 */
import java.io.IOException;
import java.util.*;
import java.io.File;
import java.util.Iterator;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;


public class JAXBExample1 {
    Map<testcase,String> hashmap=new HashMap<testcase,String>();
    public Map<testcase,String>  summaryTestcase(List<testsuite> testsuites){
        for(testsuite testsuite:testsuites){
            if(testsuite.testcase!=null){
                for(testcase ts:testsuite.testcase) {
                    hashmap.put(ts,testsuite.name);
                }
            }
            if(testsuite.testsuite!=null){
                summaryTestcase(testsuite.testsuite);
            }
        }

        return  hashmap;
    }
    public  String trimxml(String s){
        s=s.replaceAll("&nbsp;","");
        String newStr="";
        Pattern pat = Pattern.compile("\\<li\\>(.*)\\</li\\>");

        Matcher ma =  pat.matcher(s);
        int i=0;
        while(ma.find()){
            i=i+1;
            char c1=(char) (i+64);
            newStr +=c1+"  "+ma.group(1)+"\r\n";

        }
        newStr = newStr.replaceAll("&nbsp;", "");
        return newStr;
    }
    public void writer(Map<testcase,String> hashmap) throws IOException{
        Workbook wb = null;
        File file = new File("test.xls");
        Sheet sheet =null;
        wb = new HSSFWorkbook();
        sheet = (Sheet) wb.createSheet("sheet1");
        OutputStream outputStream = new FileOutputStream("test.xls");
        wb.write(outputStream);
        outputStream.flush();
        outputStream.close();
        if (sheet==null) {
            sheet = (Sheet) wb.createSheet("sheet1");
        }
        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        row.setHeight((short) 540);
        cell.setCellValue("测试用例清单");    //创建第一行

        CellStyle style = wb.createCellStyle(); // 样式对象测试用例清单
        // 设置单元格的背景颜色为淡蓝色
        style.setFillForegroundColor(HSSFColor.PALE_BLUE.index);

        style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);// 垂直
        style.setAlignment(CellStyle.ALIGN_CENTER);// 水平
        style.setWrapText(true);// 指定当单元格内容显示不下时自动换行
        CellStyle style1 = wb.createCellStyle(); // 样式对象测试用例清单
        // 设置单元格的背景颜色为淡蓝色
        style1.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
        style1.setFillForegroundColor(HSSFColor.DARK_RED.index);

        style1.setVerticalAlignment(CellStyle.VERTICAL_CENTER);// 垂直
        style1.setAlignment(CellStyle.ALIGN_LEFT);// 水平
        style1.setWrapText(true);// 指定当单元格内容显示不下时自动换行

        cell.setCellStyle(style); // 样式，居中

        Font font = wb.createFont();
        font.setBoldweight(Font.BOLDWEIGHT_BOLD);
        font.setFontName("宋体");
        font.setFontHeight((short) 280);
        style.setFont(font);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));
        sheet.autoSizeColumn(5200);
        row = sheet.createRow(1);    //创建第二行
        String titleRow[] = {"总结","测试步骤","期待结果","父节点"};
        for(int i = 0;i < titleRow.length;i++){
            cell = row.createCell(i);
            cell.setCellValue(titleRow[i]);
            cell.setCellStyle(style); // 样式，居中
            sheet.setColumnWidth(i, 50 * 256);
        }
        row.setHeight((short) 540);
        hashmap.get("0");
        Iterator iter = hashmap.entrySet().iterator();
        int i=1;


         while (iter.hasNext()) {
                i=i+1;
                row = (Row) sheet.createRow(i);
                row.setHeight((short) 1600);
                Map.Entry entry = (Map.Entry) iter.next();
                testcase key = (testcase)entry.getKey();
                String val = (String)entry.getValue();
             Cell cell1=row.createCell(0);
             cell1.setCellStyle(style1);
             cell1.setCellValue(new HSSFRichTextString(trimxml(key.getSummary())));
             Cell cell2=row.createCell(1);
             cell2.setCellStyle(style1);
             cell2.setCellValue(new HSSFRichTextString(trimxml(key.getSteps())));
             Cell cell3=row.createCell(2);
             cell3.setCellStyle(style1);
             cell3.setCellValue(new HSSFRichTextString(trimxml(key.getExpectedresults())));
             row.createCell(3).setCellValue(val);
        }


        //创建文件流
        OutputStream stream = new FileOutputStream("test.xls");
        //写入数据
        wb.write(stream);
        //关闭文件流
        stream.close();


    }
    public static void main(String[] args) {

        try {

            File file = new File("D:\\Downloads\\testsuites (2).xml");
            JAXBContext jaxbContext = JAXBContext.newInstance(testsuite.class);

            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            testsuite testsuite = (testsuite) jaxbUnmarshaller.unmarshal(file);
            System.out.println(testsuite);
            JAXBExample1 jx=new JAXBExample1();
            List<testsuite> list=new ArrayList<testsuite>();
            list.add(testsuite);
            jx.summaryTestcase(list);
            jx.writer(jx.hashmap);
            System.out.println("aaaaaaaaaaaaa"+jx.hashmap.size());

        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
