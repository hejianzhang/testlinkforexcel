package com.sky.testlinkconvert;

import java.awt.Color;
import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

/**
 * testlinkconvert的图形界面
 * @author Rachel.Luo
 * */
public class ConvertGui implements ActionListener{
	
	JTextField ja1 = new JTextField(20);
	FileDialog fd = null;
	JFrame jf = null;
	JButton jb1 =null;
	JButton jb2 = null;
	JButton jb3 = null;
	String oldfilename;
	boolean isExcelToXml=false;
	
    public ConvertGui(){
    	jf = new JFrame("Testlink转换器");
    	fd = new FileDialog(jf);
    	JPanel j1 = new JPanel();
    	JPanel j2 = new JPanel();
    	JLabel jl1 = new JLabel("源文件:");
    	jb1 = new JButton("选择");
    	jb2 = new JButton("xml转成excel");
    	jb3 = new JButton("excel转成xml");
    	jb1.addActionListener(this);
    	jb2.addActionListener(this);
    	jb3.addActionListener(this);
    	j1.add(jl1);
    	ja1.setEditable(false);
    	ja1.setBackground(Color.white);
    	j1.add(ja1);
    	j1.add(jb1);
    	jb2.setEnabled(false);
    	jb3.setEnabled(false);
    	j2.add(jb2);
    	j2.add(jb3);
    	jf.add(j1,"North");
    	jf.add(j2);
		jf.setLocation(300, 200);
    	jf.setVisible(true);
    	jf.pack();
    	jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
    
	public static void main(String[] args) {
		new ConvertGui();
	}
	
	public void actionPerformed(ActionEvent e) {
		String comm = e.getActionCommand();
		if(comm.equals("选择")){
			fd.setVisible(true);
			if(fd.getFile()!=null){
				if(fd.getFile().endsWith(".xml")||fd.getFile().endsWith(".xls")
						||fd.getFile().endsWith(".xlsx")){
					ja1.setText(fd.getDirectory()+fd.getFile());
					oldfilename=fd.getDirectory()+fd.getFile();
					
					if(fd.getFile().endsWith(".xml")){
						jb2.setEnabled(true);
						jb3.setEnabled(false);
					}else{
						jb3.setEnabled(true);
						jb2.setEnabled(false);
					}
				}else{
					JOptionPane.showMessageDialog(ja1,"请重新选择xml或excel文件！");
					ja1.setText("");
					jb2.setEnabled(false);
					jb3.setEnabled(false);
				}
			}
		}else if(comm.equals("xml转成excel")){
			isExcelToXml=false;
			jb2.setEnabled(false);
			jb1.setEnabled(false);
			new FileTransferTool(ja1,jb1,jb2,jb3,oldfilename,isExcelToXml).start();
		}else{
			isExcelToXml=true;
			jb3.setEnabled(false);
			jb1.setEnabled(false);
			new FileTransferTool(ja1,jb1,jb2,jb3,oldfilename,isExcelToXml).start();
		}
	}
}
