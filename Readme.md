## About
This is the revamped Bioimage toolbox where I copy all the files needed from the original toolbox of the plugins  that we want to keep.

**It is very much work in progress**

**For now: I will keep the folder structure exactly the same and not modify the files yet-> makes it easier to do the extraction of necessary files (by comparing the folders)**

Next step will be updating some of the files

#### TODO
* bat files, license
* tests
* pom: add our names, upgrade to 30, remove unneeded dependenies
* modify config file
* add to docs: The "parametric images" from the LabelAnalyzer can be replaced with "MorpholibJ -> LabelImages > Assign Measure To Label". the we don't need the label analyzer any more
* move the pixcount function fo labelanalyser to constrain label map , then delete file
* LabelAnalyser is strangely itnertwined with ConstraintLabelMap (-> getFeatures)

