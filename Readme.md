## About
This is the revamped Bioimage toolbox where I copy all the files needed from the original toolbox of the plugins  that we want to keep.

**It is very much work in progress**

**For now: I will keep the folder structure exactly the same and not modify the files yet-> makes it easier to do the extraction of necessary files (by comparing the folders)**

Next step will be updating some of the files

#### TODO
* bat files, license
* tests
* pom: add our names, upgrade to 2.0, remove unneeded dependenies
* modify config file
* add to docs: The "parametric images" from the LabelAnalyzer can be replaced with "MorpholibJ -> LabelImages > Assign Measure To Label". the we don't need the label analyzer any more
* move the pixcount function fo labelanalyser to constrain label map , then delete file
* LabelAnalyser is strangely itnertwined with ConstraintLabelMap (-> getFeatures)
* **AutoDisplayRange seems broken - even in older SCF versions (I checked 1.3.2)**
* **I broke the LabelAnalyzer - but since we remove it anyway there is no real point in fixing it (reason is easy: the plugin class was already removed, I just still have the underlying workhorse classes**
* make folder hierarchy flatter?

#### Removed plugins (from config file)
* "Threshold to LabelMap": ThesholdLabelinPlugin
* "Difference of Gaussian": DifferenceOfGaussianBasedLabelingPlugin
* "Watershed with seed points (2D, 3D)": WatershedLabelingPlugin
* Experimental: "Ops Label Analyser (2D, 3D) *": OpsLabelAnalyserPlugin
* Exerimental: "Particle Analyser_": ParticleAnalyserIJ1Plugin
* "Auto Display Range": AutoDisplayRangePlugin


#### Other removed plugins (maybe they are IJ2 and appeared automatically?)
* Volume Manager

