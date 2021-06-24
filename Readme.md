## Important
**This repository will be the revamped BioImageAnalysisToolbox where only selected plugins are being kept.  
*It is very much work in progress and not deployed yet (as of June 2021).***

The original toolbox can be found here: https://github.com/mpicbg-scicomp/BioImageAnalysisToolbox.


## Steps that were done so far
* The repo was initiated as copy of the [BioImageAnalysisToolbox repo](https://github.com/mpicbg-scicomp/BioImageAnalysisToolbox) (loss of git hist but the other toolbox was already with little git history...)
* Folder, package, plugin names and menu names were still kept identical up to now to ease comparing with the original repo.
* All plugin classes that were not used any more were removed. Other classes which had become unused through this were removed as well.
	* For a list of remaining plugins: see folder *fijiplugins.ui* or the *plugins.config* in the *resources* folder
* Tests corresponding to removed java classes were removed as well (+also the obsolete resource images).
* pom.xml files was updated:
	* Renamed artifact Id (jar file name) and project name.
	* Increased version number
	* Added new author names
	* removed unneeded dependencies

## Next steps: 
* ToDo list and discussion on open questions is in the [CBG gitlab](https://git.mpi-cbg.de/scicomp/bioimage_team/operations/-/issues/86) (internal access only).
* Next important point on the ToDo list: package naming and menu-entry naming, such that new and old toolbox can be deployed at the same time without conflict during the transition phase. 


## -----------------------------------------
#### For the future:
#### About
This is a toolbox with Fiji plugins for generic image processing, developed by the [Scientific Computing Facility](https://www.mpi-cbg.de/services-facilities/core-facilities/scientific-computing-facility/service-portfolio-overview/) at the MPI-CBG Dresden. The plugins will be distributed via the *SCF_MPI_CBG* update site.

#### Toolbox Documentation
* Documentation of all plugins, including the deprecated ones can be found in the CBG wiki (account required): https://wiki.mpi-cbg.de/scicomp/SCF 
* TODO: make documentation of kept plugins available publicly.

