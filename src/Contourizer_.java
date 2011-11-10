/*
*   ContourPlot  -- A simple 2D contour plot of gridded 3D data.
*
*   Copyright (C) 2000-2002 by Joseph A. Huwaldt <jhuwaldt@knology.net>.
*   All rights reserved.
*   
*   This library is free software; you can redistribute it and/or
*   modify it under the terms of the GNU Library General Public
*   License as published by the Free Software Foundation; either
*   version 2 of the License, or (at your option) any later version.
*   
*   This library is distributed in the hope that it will be useful,
*   but WITHOUT ANY WARRANTY; without even the implied warranty of
*   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
*   Library General Public License for more details.
**/
//package jahuwaldt.plot;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Line;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import ij.process.ShortProcessor;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import java.util.*;
import java.text.NumberFormat;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.factories.FormFactory;



/**
*  <p> An object that represents a simple 2D contour plot
*      of 3D data.
*  </p>
*  <p> Grid styles, axis formats, and axis labels can
*      be changed by accessing this object's axes and
*      making the changes there.  Then call repaint()
*      on the component containing this plot.
*  </p>
*  <p> This object's run list contains the contour lines
*      being plotted.  Each run represents a contour level.
*  </p>
*
*  <p>  Modified by:  Joseph A. Huwaldt  </p>
*
*  @author  Joseph A. Huwaldt   Date:  November 12, 2000
*  @version November 20, 2000
**/
public class Contourizer_ implements PlugIn{

	//	Debug flag.
	private static final boolean DEBUG = false;
	
	//	The contour paths displayed in this plot.
	private ContourPath[] paths = null;
	private int WIDTH,HEIGHT, DEPTH,BITDEPTH;
	private ImagePlus currImp;
	private String name="";
	private static float PAS=2;
	private int nc=10;
	private String thresholdMode="Otsu";
	public static void main(String[] args){}
 
	@Override
	public void run(String arg0) {
		//launchCalculation();
	    createAndShowGUI();

	}
	
	/**
	 * @wbp.parser.entryPoint
	 */
	private void createAndShowGUI() {
		final JFrame jf=new JFrame("Contourizer");
		jf.setSize(317, 203);
		jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		JPanel mainPanel = new JPanel();
		jf.getContentPane().add(mainPanel, BorderLayout.CENTER);
		mainPanel.setLayout(null);
		
		JLabel lblNumberOfContours = new JLabel("Number of contours");
		lblNumberOfContours.setBounds(19, 0, 125, 44);
		mainPanel.add(lblNumberOfContours);
		
		final JSpinner nbContSpinner = new JSpinner();
		nbContSpinner.setBounds(163, 12, 118, 20);
		nbContSpinner.setModel(new SpinnerNumberModel(new Integer(10), new Integer(1), null, new Integer(1)));
		mainPanel.add(nbContSpinner);
		
		JLabel lblAutoAdj = new JLabel("Contour scale");
		lblAutoAdj.setBounds(38, 49, 87, 31);
		mainPanel.add(lblAutoAdj);
		
		final JComboBox scaleCont = new JComboBox();
		scaleCont.setBounds(163, 52, 118, 24);
		scaleCont.setModel(new DefaultComboBoxModel(new String[] {"Otsu", "None"}));
		mainPanel.add(scaleCont);
		
		JCheckBox chckbxShowstringchk = new JCheckBox("Show Strings");
		chckbxShowstringchk.setBounds(28, 87, 106, 23);
		mainPanel.add(chckbxShowstringchk);
		
		JButton btnOk = new JButton("OK");
		btnOk.setBounds(56, 117, 50, 25);
		mainPanel.add(btnOk);
		btnOk.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) {
				SpinnerNumberModel model = (SpinnerNumberModel)nbContSpinner.getModel();
				nc = model.getNumber().intValue();
				thresholdMode=(String)scaleCont.getSelectedItem();
				jf.dispose();
				launchCalculation();
			}
		});
		
		JButton btnClose = new JButton("Close");
		btnClose.setBounds(188, 117, 68, 25);
		mainPanel.add(btnClose);
		btnClose.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) {
				jf.dispose();
			}
		});
		jf.setVisible(true);
	}
	
	public void launchCalculation(){
		currImp=ij.WindowManager.getCurrentImage();
		name=currImp.getTitle();
		WIDTH=currImp.getWidth();
		HEIGHT=currImp.getHeight();
		DEPTH=currImp.getNSlices();
		BITDEPTH=currImp.getBitDepth();
		ij.IJ.showProgress(0, DEPTH);
		double[][] zArr=new double[(int) Math.ceil(WIDTH/PAS)][(int) Math.ceil(HEIGHT/PAS)];
		double[][] xArr=new double[(int) Math.ceil(WIDTH/PAS)][(int) Math.ceil(HEIGHT/PAS)];
		double[][] yArr=new double[(int) Math.ceil(WIDTH/PAS)][(int) Math.ceil(HEIGHT/PAS)];
		ImageStack ims=new ImageStack(WIDTH,HEIGHT);
		ByteProcessor bp;
		ShortProcessor sp;
		ImageStack imsin=currImp.getImageStack();
		for(int d=0;d<DEPTH;d++){
			short[] sArr=null;
			byte[] bArr=null;
			switch(BITDEPTH){
			case 8:
				bArr=(byte[]) ((ByteProcessor)imsin.getProcessor(d+1)).getPixels();
				break;
			case 16:
				sArr=(short[]) ((ShortProcessor)imsin.getProcessor(d+1)).getPixels();
				break;
			
			}
			int c1=0;int c2=0;
			for(int i=0;i<WIDTH;i+=PAS){
				c2=0;
				for(int j=0;j<HEIGHT;j+=PAS){ 
					xArr[c1][c2]=i+1;
					yArr[c1][c2]=j+1;
					switch(BITDEPTH){
						case 8: zArr[c1][c2]=bArr[(j)*WIDTH+i]&0xff;break;
						case 16: zArr[c1][c2]=sArr[(j)*WIDTH+i];break;
					}
					c2++;
				}
				c1++;
			}
			bArr=null;
			switch(BITDEPTH){
				case 8: 
					bp=(ByteProcessor) createPlot(xArr, yArr, zArr, "x", "y", null, null, nc, false);
					ims.addSlice(""+d, bp);
					break;
				case 16:
					sp=(ShortProcessor) createPlot(xArr, yArr, zArr, "x", "y", null, null, nc, false);
					ims.addSlice(""+d, sp);
					break;
			}
			
			ij.IJ.showProgress(d+1, DEPTH);
		}
		imsin=null;
		currImp=null;
		ImagePlus contourImage=new ImagePlus(name+" contour", ims);
		contourImage.show();
	}
	public Object createPlot( double[][] xArr, double[][] yArr, double[][] zArr,
						String xLabel, String yLabel,
						NumberFormat xFormat, NumberFormat yFormat,
						int nc, boolean logIntervals) {
		try {
		
			//	Generate the contours.
			ContourGenerator cg = new ContourGenerator(xArr, yArr, zArr, nc, logIntervals, currImp, thresholdMode);
			paths = cg.getContours();
			int npaths = paths.length;
		
			if (DEBUG) {
				System.out.println("Number of contours = " + nc);
				System.out.println("Number of contour paths = " + npaths);
			}
			
			byte[] bcontourPixels;
			short[] scontourPixels;
			ByteProcessor bp=null;
			ShortProcessor sp=null;
			switch(BITDEPTH){
				case 8:
					bp=new ByteProcessor(WIDTH, HEIGHT);
					bcontourPixels=new byte[WIDTH*HEIGHT];
					for(int i=0;i<bcontourPixels.length;i++) bcontourPixels[i]=(byte) 255;
					bp.setPixels(bcontourPixels);
					break;
				case 16: 
					sp=new ShortProcessor(WIDTH, HEIGHT);
					scontourPixels=new short[WIDTH*HEIGHT];
					for(int i=0;i<scontourPixels.length;i++) scontourPixels[i]=(short) 255;
					sp.setPixels(scontourPixels);
					break;
			}
			//	Loop over all the contour paths, adding them to the appropriate
			//	contour level.
			for (int j=0; j < npaths; ++j) {
				//	Retrieve the contour path data.
				double[] xData = paths[j].getAllX();
				double[] yData = paths[j].getAllY();
				int levelIndex = paths[j].getLevelIndex();
			
				if (DEBUG) {
					System.out.println();
					System.out.println("LevelIdx = " + levelIndex);
				}
				
				int numPoints = xData.length;				
				for (int i=1; i < numPoints; ++i) {
					switch(BITDEPTH){
						case 8:
							bp.setColor(levelIndex*10);
							bp.drawLine((int)Math.round(xData[i-1]), (int)Math.round(yData[i-1]), (int)Math.round(xData[i]), (int)Math.round(yData[i]));
							break;
						case 16:
							sp.setColor(levelIndex*10);
							sp.drawLine((int)Math.round(xData[i-1]), (int)Math.round(yData[i-1]), (int)Math.round(xData[i]), (int)Math.round(yData[i]));
							break;
					}
					if (DEBUG)
						System.out.println("X = " + (float)xData[i] + ",  Y = " + (float)yData[i]);
				}
				
			}
			switch(BITDEPTH){
				case 8: return bp;
				case 16: return sp;
			}
			return null;
		} catch (InterruptedException e) {
			//	Shouldn't be possible here.
			e.printStackTrace();
			return null;
		}		
	}

	/**
	*  Colorize the contours by linearly interpolating between
	*  the specified colors for this plot's range of contour levels.
	**/
	public void colorizeContours(Color lowColor, Color highColor) {
		
		//	Find the range of levels in the contours.
		double minLevel = Double.MAX_VALUE;
		double maxLevel = -minLevel;
		int npaths = paths.length;
		for (int i=0; i < npaths; ++i) {
			double level = paths[i].getAttributes().getLevel();
			minLevel = Math.min(minLevel, level);
			maxLevel = Math.max(maxLevel, level);
		}
	}
	
	/**
	*  Interpolate the colors for the contour level.
	**/
	private Color interpColors(Color lowColor, Color highColor,
						double minLevel, double maxLevel, double level) {
		level -= minLevel;
		double range = maxLevel - minLevel;
		double temp = range - level;
		
		Color color = new Color(
			(int)(( temp*lowColor.getRed() + level*highColor.getRed() )/range),
			(int)(( temp*lowColor.getGreen() + level*highColor.getGreen() )/range),
			(int)(( temp*lowColor.getBlue() + level*highColor.getBlue() )/range) );
		
		return color;
	}



}

