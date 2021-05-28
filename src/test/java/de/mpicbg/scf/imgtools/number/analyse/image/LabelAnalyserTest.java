package de.mpicbg.scf.imgtools.number.analyse.image;

import de.mpicbg.scf.imgtools.number.analyse.array.Equal;
import de.mpicbg.scf.imgtools.number.analyse.image.LabelAnalyser.Feature;
import de.mpicbg.scf.imgtools.number.filter.ArrayUtilities;
import de.mpicbg.scf.imgtools.ui.DebugHelper;
import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.stats.ComputeMinMax;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.roi.Regions;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Test class for the label based particle analyser
 * <p>
 * TODO: Build in reference values.
 * TODO: Remove call to dirty workaround in LabelMoments3D
 * <p>
 * <p>
 * <p>
 * Author: Robert Haase, Scientific Computing Facility, MPI-CBG Dresden,
 * rhaase@mpi-cbg.de
 * Date: November 2015
 * <p>
 * Copyright 2017 Max Planck Institute of Molecular Cell Biology and Genetics,
 * Dresden, Germany
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
public class LabelAnalyserTest {

    @Test
    public void academicTestExample() {
        // --------------------------------------
        // Open an image, in this case a labelMap
        ImagePlus imp = IJ.openImage("src/test/resources/surface2D_labelmap.tif");
        Img<IntType> img = ImageJFunctions.wrapReal(imp);

        // --------------------
        // Get image properties
        Calibration calib = imp.getCalibration();
        double[] voxelSize = new double[]{calib.pixelWidth, calib.pixelHeight, calib.pixelDepth};


        LabelAnalyser<IntType, IntType> lpa = new LabelAnalyser<IntType, IntType>(img, voxelSize, new Feature[]{Feature.AREA_VOLUME,
                Feature.MEAN});


        lpa.setSignalImage(img);

        // Most features only have one dimension, e.g. for every label, there is only on average signal. These features can be accessed by
        double[] averageSignal = lpa.getFeatures(Feature.MEAN);

        DebugHelper.print(this, "Avg:\n" + Arrays.toString(averageSignal));


        imp.close();
        DebugHelper.print(this, "Test finished");
    }

    @Test
    public void testImgLabelingConversionUsingBinaryMaskRegionOfInterest() {
        ImagePlus imp = IJ.openImage("src/test/resources/labelmaptest.tif");

        ImgLabeling<Integer, IntType> labeling = LabelAnalyser.getIntIntImgLabellingFromLabelMapImg(ImageJFunctions.convertFloat(imp));

        Img<FloatType> img = ImageJFunctions.convertFloat(imp);

        long[] labelPixelCount = LabelAnalyser.getLabelsPixelCount(img);

        LabelRegions<Integer> regions = new LabelRegions<Integer>(labeling);

        Set<Integer> labelNames = labeling.getMapping().getLabels();

        DebugHelper.print(this, "Numer of labels: " + labelNames.size());

        for (Integer labelName : labelNames) {
            LabelRegion<Integer> lr = regions.getLabelRegion(labelName);

            IterableInterval<FloatType> ii = Regions.sample(lr, img);

            FloatType min = img.cursor().get().createVariable();
            FloatType max = img.cursor().get().createVariable();

            ComputeMinMax<FloatType> cmm = new ComputeMinMax<FloatType>(ii, min, max);
            cmm.process();
            DebugHelper.print(this, "Label " + labelName + " has a minimum of " + cmm.getMin().get() + " and a maximum of " + cmm.getMax().get());
            assertTrue("Label minimum in labelmap equals label name ", cmm.getMin().get() == labelName);
            assertTrue("Label maximum in labelmap equals label name ", cmm.getMax().get() == labelName);

            DebugHelper.print(this, "Label " + labelName + " has " + ii.size() + " elements " + labelPixelCount[labelName - 1]);
            assertTrue("Label pixel count  is correct", ii.size() == labelPixelCount[labelName - 1]);

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        imp.close();
    }


    @Test
    public void testIfLabelPixelCountIsEqualToMoments() {
        DebugHelper.print(this, "Testing moments...");
        //new ij.ImageJ();
        ImagePlus imp = IJ.openImage("src/test/resources/labelmaptest.tif");

        Img<FloatType> img = ImagePlusAdapter.wrap(imp);
        long[] counts = LabelAnalyser.getLabelsPixelCount(img);

        Interval bb = Intervals.createMinMax(new long[]{0, 0, 0, imp.getWidth() - 1, imp.getHeight() - 1, imp.getNSlices() - 1});

        for (int i = 0; i < Math.min(10, counts.length); i++) {
            LabelMoments3D<FloatType, FloatType> lm3d = new LabelMoments3D<FloatType, FloatType>(img, i + 1, bb, new double[]{1, 1, 1}, 2);

            double[][][] moments = lm3d.getMoments();

            DebugHelper.print(this, "Pixelcount[" + i + "] match to moment " + counts[i] + " == " + moments[0][0][0]);
            assertTrue("Pixelcount[" + i + "] match to moment " + counts[i] + " == " + moments[0][0][0], counts[i] == moments[0][0][0]);
        }

        imp.close();
    }


    @Test
    public void testIfLabelPixelCountIsCorrect() {
        ImagePlus imp = IJ.openImage("src/test/resources/labelmaptest.tif");

        Img<FloatType> img = ImagePlusAdapter.wrap(imp);
        long[] counts = LabelAnalyser.getLabelsPixelCount(img);

        long[] references = {
                100 * 10,
                400 * 20,
                200 * 20,
                1600 * 20,
                316 * 20,
                4 * 20,
                300 * 20,
                200 * 20,
                210 * 20,
                413 * 20,
                574 * 20,
                645 * 20,
                481 * 20,
                385 * 20,
                379 * 20,
                566 * 20,
                444 * 20,
                241 * 20,
                199 * 20,
                172 * 20,
                214 * 20,
                312 * 20,
                249 * 20,
                167 * 20,
                210 * 20,
                200 * 20,
                200 * 20,
                200 * 20,
                200 * 20,
                200 * 20,
                200 * 20,
                200 * 20
        };

        assertTrue("Number of array elements and array entries equal", counts.length == references.length);

        for (int i = 0; i < counts.length; i++) {
            assertTrue("PixelCount " + i + " is correct " + counts[i] + " == " + references[i], counts[i] == references[i]);
        }

        imp.close();
    }




}
