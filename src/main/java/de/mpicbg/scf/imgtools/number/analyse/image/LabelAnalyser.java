package de.mpicbg.scf.imgtools.number.analyse.image;

import de.mpicbg.scf.imgtools.ui.DebugHelper;
import java.util.Arrays;
import java.util.EnumSet;
import net.imglib2.*;
import net.imglib2.algorithm.stats.ComputeMinMax;
import net.imglib2.img.Img;
import net.imglib2.roi.Regions;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

/**
 * Update (2021-05) : Simplified LabelAnalyzer to only compute features that are needed on ConstraintLabelMap:
 *      Area/Volume and Mean
*
 * <p>
 * Example code can be found in LabelParticleAnalyserTest
 * <p>
 * <p>
 * <p>
 * <p>
 * Author: Robert Haase, Scientific Computing Facility, MPI-CBG Dresden,
 * rhaase@mpi-cbg.de
 * Date: March 2016
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
 *
 * @param <I> Type of the LabelMap
 * @param <F> Type of the image where signal measures are performed on.
 */
public class LabelAnalyser<I extends RealType<I>, F extends RealType<F>> {
    public enum Feature {
        AREA_VOLUME("Area / volume"),
        MEAN("Mean average signal");

        private final String name;

        Feature(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }

    }

    // Input:
    private EnumSet<Feature> whatToMeasure = null;

    private final Img<I> labelMap;
    private Img<F> signalMap;

    private final double[] voxelSize;

    // State:
    private boolean resultsValid = false;

    // Output:
    private int numLabels = 0;

    private double[] volumes = null;
    private double[] averages = null;


    public LabelAnalyser(Img<I> labelMap, double[] voxelSize, Feature[] featuresToExtract) {
        this.labelMap = labelMap;
        this.whatToMeasure = EnumSet.copyOf(Arrays.asList(featuresToExtract));
        this.voxelSize = voxelSize;
    }

    public LabelAnalyser(Img<I> labelMap, double[] voxelSize, EnumSet<Feature> featuresToExtract) {
        this.labelMap = labelMap;
        this.whatToMeasure = featuresToExtract;
        this.voxelSize = voxelSize;
    }

    public void setSignalImage(Img<F> signalImage) {
        this.signalMap = signalImage;
        resultsValid = false;
    }


    private void doFeatureExtaction() {
        if (resultsValid) {
            return;
        }

        // ------------------------
        // reset
        volumes = null;
        averages = null;

        // ------------------------------------------------------------------------------------
        // Prepare: Get label map and signal image in ImgLib2 format, read out sizes, dimensions, number of labels
        LabelRegions<Integer> regions = null;

        Interval[] boundingIntervals = LabelAnalyser.getLabelsBoundingIntervals(labelMap);

        int numDimensions = labelMap.numDimensions();
        DebugHelper.print(this, "numDimensions " + numDimensions);
        DebugHelper.print(this, "numLabels " + numLabels);

        numLabels = boundingIntervals.length;

        // -------------------------------------------------------------
        // prepare: Create memory for all deserved parameters
        if (whatToMeasure.contains(Feature.AREA_VOLUME)) {
            volumes = new double[numLabels];
        }
        if (whatToMeasure.contains(Feature.MEAN)) {
            averages = new double[numLabels];
        }

        // ---------------------------------------------------------------------------------------
        // Go through all labels and determine parameters (which were not determined so far)
        for (int i = 0; i < numLabels; i++) {
            // TODO: since we only have volumes left for calculation could simplify the LabelMoments3D
            LabelMoments3D<I, F> lm3d = new LabelMoments3D<I, F>(labelMap, i + 1, boundingIntervals[i], voxelSize, 2);

            if (volumes != null) {
                volumes[i] = lm3d.getMoments()[0][0][0];
            }

            if (signalMap != null) {
                if (averages != null ) {

                    if (averages != null) {
                        double volume = lm3d.getMoments()[0][0][0];
                        lm3d.setSignalImage(signalMap);
                        averages[i] = lm3d.getMoments()[0][0][0] / volume;
                    } else {
                        lm3d.setSignalImage(signalMap);
                    }
                }

                if (regions != null) {
                    LabelRegion<Integer> lr = regions.getLabelRegion(i + 1);
                    IterableInterval<F> ii = Regions.sample(lr, signalMap);

                    F min = signalMap.cursor().next().copy();
                    F max = signalMap.cursor().next().copy();

                    ComputeMinMax<F> cmm = new ComputeMinMax<F>(ii, min, max);
                    cmm.process();
                }
            }
        }
        resultsValid = true;
    }


    public double[] getFeatures(Feature measurement) {
        doFeatureExtaction();

        switch (measurement) {
            case AREA_VOLUME:
                return volumes;
            case MEAN:
                return averages;
           default:
                return null;
        }
    }

    /**
     * Returns a histogram of all pixels in the image. In fact, the indexes of
     * the histogram are the rounded (rather floored) pixel signal values.
     *
     * @param img ImgLib2 Img to be processed.
     * @param <T> pixel type of the image
     * @return returns an array containing (int)max grey value elements.
     */
    public static <T extends RealType<T>> long[] getLabelsPixelCount(Img<T> img) {

        Cursor<T> cursor = img.cursor();
        int max = 0;
        while (cursor.hasNext()) {
            int val = (int) cursor.next().getRealFloat();
            if (val > max) {
                max = val;
            }
        }

        long[] volumes = new long[max];
        cursor.reset();
        while (cursor.hasNext()) {
            int val = (int) cursor.next().getRealFloat();
            if (val > 0) {
                volumes[val - 1]++;
            }
        }

        return volumes;
    }


    public static <T extends RealType<T>> long[][] getLabelsBoundingBoxes(Img<T> labelMap) {
        Cursor<T> cursor = labelMap.cursor();
        int max = 0;
        while (cursor.hasNext()) {
            int val = (int) cursor.next().getRealFloat();
            if (val > max) {
                max = val;
            }
        }

        int numDimensions = labelMap.numDimensions();

        long[][] minmaxintervals = new long[max][numDimensions * 2];
        cursor.reset();

        boolean[] intervalInitialized = new boolean[max];

        while (cursor.hasNext()) {
            int idx = (int) cursor.next().getRealFloat() - 1;
            if (idx >= 0) {
                if (!intervalInitialized[idx]) {
                    for (int d = 0; d < numDimensions; d++) {
                        long position = cursor.getLongPosition(d);
                        // min
                        minmaxintervals[idx][d] = position;
                        // max
                        minmaxintervals[idx][d + numDimensions] = position;
                    }
                    intervalInitialized[idx] = true;
                }

                for (int d = 0; d < numDimensions; d++) {
                    long position = cursor.getLongPosition(d);
                    // min
                    if (minmaxintervals[idx][d] > position) {
                        minmaxintervals[idx][d] = position;
                    }
                    // max
                    if (minmaxintervals[idx][d + numDimensions] < position) {
                        minmaxintervals[idx][d + numDimensions] = position;
                    }
                }
            }
        }

        return minmaxintervals;
    }

    public static <T extends RealType<T>> Interval[] getLabelsBoundingIntervals(Img<T> labelMap) {

        long[][] boundingBoxes = LabelAnalyser.getLabelsBoundingBoxes(labelMap);
        Interval[] intervals = new Interval[boundingBoxes.length];

        for (int i = 0; i < intervals.length; i++) {
            intervals[i] = Intervals.createMinMax(boundingBoxes[i]);
        }
        return intervals;
    }


    public static <T extends RealType<T>> ImgLabeling<Integer, IntType> getIntIntImgLabellingFromLabelMapImg(Img<T> labelMap) {
        final Dimensions dims = labelMap;
        final IntType t = new IntType();
        final RandomAccessibleInterval<IntType> img = Util.getArrayOrCellImgFactory(dims, t).create(dims, t);
        final ImgLabeling<Integer, IntType> labeling = new ImgLabeling<Integer, IntType>(img);

        final Cursor<LabelingType<Integer>> labelCursor = Views.flatIterable(labeling).cursor();

        for (final T input : Views.flatIterable(labelMap)) {
            final LabelingType<Integer> element = labelCursor.next();
            if (input.getRealFloat() != 0) {
                element.add((int) input.getRealFloat());
            }
        }
        return labeling;
    }

}