/*
*   Contourizer, an ImageJ plugin to make contours plots
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
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.ContrastEnhancer;
import ij.plugin.PlugIn;
import ij.plugin.frame.ContrastAdjuster;
import ij.process.ByteProcessor;
import ij.process.ShortProcessor;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import java.util.*;
import java.util.List;
import java.text.NumberFormat;



/**
 * ImageJ plugin to automaticaly retrieve contour from
 * Image data, within or not the selected ROI
 * 
 * 
 *  Based on the java code from : Joseph A. Huwaldt  
 *	Update to make an ImageJ plugin by Jérémy DEVERDUN
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
	private boolean hasRoi=false;
	private double zMax=Double.MIN_VALUE;
	private double zMin=Double.MAX_VALUE;
	private boolean logInterval=false;
	private ImageStack ims=null;
	private boolean showLevel=true;
	private ResultsTable rt;

	private boolean smoothResult=true;
	
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
		GenericDialog g=new GenericDialog("Contourizer");
		g.addSlider("Number of contours", 2, 100, 10);
		g.addChoice("Contour scale", new String[] {"Otsu","None"}, "Otsu");
		g.addCheckboxGroup(2, 2, new String[]{"Show Levels","Log Interval","Smooth results"}, new boolean[]{true,true,false});
		g.showDialog();
		if (g.wasCanceled()) return;
	    nc=(int) Math.round(g.getNextNumber());
	    thresholdMode=g.getNextChoice();
	    showLevel=g.getNextBoolean();
	    logInterval=g.getNextBoolean();
	    smoothResult=g.getNextBoolean();
		try{
			launchCalculation();
		}catch(Exception e1){
			IJ.log(e1.toString());
			currImp=null;
			ImagePlus contourImage=new ImagePlus(name+" contour", ims);
			contourImage.show();
			ij.IJ.freeMemory();
		}
	}
	
	public void launchCalculation() throws Exception{
		currImp=ij.WindowManager.getCurrentImage();
		name=currImp.getTitle();
		WIDTH=currImp.getWidth();
		HEIGHT=currImp.getHeight();
		DEPTH=currImp.getNSlices();
		BITDEPTH=currImp.getBitDepth();
		ij.IJ.showProgress(0, DEPTH);
		Roi roi=currImp.getRoi();
		hasRoi=roi!=null;
		double[][] zArr;
		double[][] xArr;
		double[][] yArr;
		// Set some variables to handle ROI
		int startX=0;
		int startY=0;
		int endX=WIDTH;
		int endY=HEIGHT;
		if(!hasRoi){
			zArr=new double[(int) Math.ceil(WIDTH/PAS)][(int) Math.ceil(HEIGHT/PAS)];
			xArr=new double[(int) Math.ceil(WIDTH/PAS)][(int) Math.ceil(HEIGHT/PAS)];
			yArr=new double[(int) Math.ceil(WIDTH/PAS)][(int) Math.ceil(HEIGHT/PAS)];
		}else{
			zArr=new double[(int) Math.ceil(roi.getBounds().width/PAS)][(int) Math.ceil(roi.getBounds().height/PAS)];
			xArr=new double[(int) Math.ceil(roi.getBounds().width/PAS)][(int) Math.ceil(roi.getBounds().height/PAS)];
			yArr=new double[(int) Math.ceil(roi.getBounds().width/PAS)][(int) Math.ceil(roi.getBounds().height/PAS)];
			startX=roi.getBounds().x;
			startY=roi.getBounds().y;
			endX=startX+roi.getBounds().width;
			endY=startY+roi.getBounds().height;
		}
		ims=new ImageStack(WIDTH,HEIGHT);
		ByteProcessor bp;
		ShortProcessor sp;
		ImageStack imsin=currImp.getImageStack();
		// get min and max in the ROI based on all slices
		if(hasRoi){
			short[] sArr=null;
			byte[] bArr=null;
			for(int d=0;d<DEPTH;d++){
				switch(BITDEPTH){
				case 8:
					bArr=(byte[]) ((ByteProcessor)imsin.getProcessor(d+1)).getPixels();
					break;
				case 16:
					sArr=(short[]) ((ShortProcessor)imsin.getProcessor(d+1)).getPixels();
					break;
				}
				for(int i=startX;i<endX;i+=PAS){
					for(int j=startY;j<endY;j+=PAS){ 
						if((roi.contains(i+1, j+1))){
							switch(BITDEPTH){
								case 8:	
									zMax=Math.max(zMax, bArr[(j)*WIDTH+i]&0xff);
									zMin=Math.min(zMin, bArr[(j)*WIDTH+i]&0xff);break;
								case 16: 
									zMax=Math.max(zMax, sArr[(j)*WIDTH+i]);
									zMin=Math.min(zMin, sArr[(j)*WIDTH+i]); break;
							}
						}
					}
				}
			}
		}
		for(int d=0;d<DEPTH;d++){
			// Test if there is enough memory
			ij.IJ.freeMemory();
			//if((ij.IJ.maxMemory()-ij.IJ.currentMemory())<50000000) throw new Exception("Not enough memory");
			short[] sArr=null;
			byte[] bArr=null;
			switch(BITDEPTH){
			case 8:
				bArr=(byte[]) ((ByteProcessor)imsin.getProcessor(d+1)).getPixels();
				if(!hasRoi){
					zMax=((ByteProcessor)imsin.getProcessor(d+1)).getMax();
					zMin=((ByteProcessor)imsin.getProcessor(d+1)).getMin();
				}
				break;
			case 16:
				sArr=(short[]) ((ShortProcessor)imsin.getProcessor(d+1)).getPixels();
				if(!hasRoi){
					zMax=((ShortProcessor)imsin.getProcessor(d+1)).getMax();
					zMin=((ShortProcessor)imsin.getProcessor(d+1)).getMin();
				}
				break;
			
			}
			int c1=0;int c2=0;
			for(int i=startX;i<endX;i+=PAS){
				c2=0;
				for(int j=startY;j<endY;j+=PAS){ 
					xArr[c1][c2]=i+1;
					yArr[c1][c2]=j+1;
					zArr[c1][c2]=255;
					if(!hasRoi || (roi.contains(i+1, j+1))){
						switch(BITDEPTH){
							case 8:	zArr[c1][c2]=bArr[(j)*WIDTH+i]&0xff;break;
							case 16: zArr[c1][c2]=sArr[(j)*WIDTH+i]; break;
						}
					}
					c2++;
				}
				c1++;
			}
			bArr=null;
			switch(BITDEPTH){
				case 8: case 16:
					bp=(ByteProcessor) createPlot(xArr, yArr, zArr, "x", "y", null, null, nc, logInterval);
					ims.addSlice(""+d, bp);
					break;
			/*	case 16:
					sp=(ShortProcessor) createPlot(xArr, yArr, zArr, "x", "y", null, null, nc, logInterval);
					ims.addSlice(""+d, sp);
					break;*/
			}
			
			ij.IJ.showProgress(d+1, DEPTH);
		}
		imsin=null;
		currImp=null;
		ImagePlus contourImage=new ImagePlus(name+" contour", ims);
		contourImage.show();
		contourImage=null;
		ij.IJ.freeMemory();
	}
	public Object createPlot( double[][] xArr, double[][] yArr, double[][] zArr,
						String xLabel, String yLabel,
						NumberFormat xFormat, NumberFormat yFormat,
						int nc, boolean logIntervals) {
		try {
		
			//	Generate the contours.
			ContourGenerator cg = new ContourGenerator(xArr, yArr, zArr, nc, logIntervals, currImp, zMin, zMax, thresholdMode);
			// display contour list
			if(showLevel){
				ContourAttrib[] cattr=cg.getcAttr();
				rt=new ResultsTable();
				rt.addColumns();
				for(int i=0;i<cattr.length;i++){
					rt.incrementCounter();
					rt.setValue(1, i, cattr[i].getLevel());
				}
				rt.show("Levels");
				showLevel=false;
			}
			
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
				case 8:case 16:
					bp=new ByteProcessor(WIDTH, HEIGHT);
					bcontourPixels=new byte[WIDTH*HEIGHT];
					for(int i=0;i<bcontourPixels.length;i++) bcontourPixels[i]=(byte) 0;
					bp.setPixels(bcontourPixels);
					break;
				/*case 16: 
					sp=new ShortProcessor(WIDTH, HEIGHT);
					scontourPixels=new short[WIDTH*HEIGHT];
					for(int i=0;i<scontourPixels.length;i++) scontourPixels[i]=(short) 0;
					sp.setPixels(scontourPixels);
					break;*/
			}
			//	Loop over all the contour paths, adding them to the appropriate
			//	contour level.
			for (int j=0; j < npaths; ++j) {
				//	Retrieve the contour path data.
				double[] xData = paths[j].getAllX();
				double[] yData = paths[j].getAllY();
				int levelIndex = paths[j].getLevelIndex();
				List<CustomLine> list=arrayToLines(xData, yData);
				if(smoothResult)
					for(int i=0;i<10;i++)
						list=LineSmoother.smoothLine(list);
				if (DEBUG) {
					System.out.println();
					System.out.println("LevelIdx = " + levelIndex);
				}
								
				for(CustomLine cl:list){
					switch(BITDEPTH){
						case 8:case 16:
							bp.setColor(levelIndex+1);//(int)Math.round((2^BITDEPTH)/rt.getValueAsDouble(1, levelIndex)*255));
							bp.drawLine(cl.x1, cl.y1, cl.x2, cl.y2);
							break;
						/*case 16:
							sp.setColor(levelIndex*10);
							sp.drawLine(cl.x1, cl.y1, cl.x2, cl.y2);
							break;*/
					}
				}
				bp.setMinAndMax(1, levelIndex+1);
			}
			switch(BITDEPTH){
				case 8: case 16: return bp;
				//case 16: return sp;
			}
			return null;
		} catch (InterruptedException e) {
			//	Shouldn't be possible here.
			e.printStackTrace();
			return null;
		}		
	}

	/**
	 * Convert an array of x and y positions to a list of customLine
	 * @param x array of x positions
	 * @param y array of y positions
	 * @return List of customLine
	 */
	private List<CustomLine> arrayToLines(double[] x,double[] y){
    	LinkedList<CustomLine> ll=new LinkedList<CustomLine>();
    	for (int i=1; i < x.length; ++i) {
			ll.add(new CustomLine((int)Math.round(x[i-1]), (int)Math.round(y[i-1]), (int)Math.round(x[i]), (int)Math.round(y[i])));
    	}
    	return ll;
    }
}

