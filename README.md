# Welcome to CellGraph (a.k.a. icy plugins for EpiTools)
---

CellGraph is a plugin collection for the bioimaging framework [icy](http://icy.bioimageanalysis.org) and part of the EpiTools project. EpiTools is an open source image analysis toolkit for quantifying epithelial growth dynamics. To know more about the project please visit our project website or the related code repositories:

* EpiTools homepage: [https://epitools.github.io](https://epitools.github.io)
* Image processing part (MATLAB): [https://github.com/epitools/epitools-matlab](https://github.com/epitools/epitools-matlab)

If you like CellGraph and use it in your work, please cite our EpiTools paper in Developmental Cell (January 2016), freely available at [http://dx.doi.org/10.1016/j.devcel.2015.12.012](http://dx.doi.org/10.1016/j.devcel.2015.12.012)


# Installation
---

For detailed instructions (and toolbar installation) we reccomend our wiki page at:
[https://epitools.github.io/wiki/Icy_Plugins/00_Installation/](https://epitools.github.io/wiki/Icy_Plugins/00_Installation/)

But in essence:

1. Download the latest cellGraph.zip from the [release section](https://github.com/epitools/epitools-icy/releases) of this repository
2. Extract the archive
3. Place the plugin folder davhelle into the icy plugin folder (e.g. programs/icy/plugins)
4. Restart icy and you should see the plugins under tab Plugins > Other Plugins > davhelle

To test the installation, a small example is included in the archive, load it by opening the TestLoader Plugin and select the "test" directory. Now you should see the following (see wiki above to add the toolbar):

![TestLoader](https://epitools.github.io/wiki/Images/icy/test_plugin.png)

# Development
---

I've been mostly developing CellGraph using the [method](http://icy.bioimageanalysis.org/index.php?display=startDevWithIcy) suggested by the icy developers, which is based on eclipse and allows launching the plugin directly from there.

To get started developing, I would suggest the following (assuming you have icy and epitools already set up):

1. Install eclipse and [icy4eclipse](http://icy.bioimageanalysis.org/index.php?display=startDevWithIcy)
2. Pull the epitools plugin master branch from this repo
3. Import the eclipse project by creating a new project from the git repository (*)
4. Launch the project through the 'D' icon in eclipse toolbar, which should automatically open icy and load the plugin
5. Use the test launcher to load the default dataset
6. Try modifying an overlay to see interactive changes in the code, for example:
	a. load the simple test_overlay via CellOverlay plugin
	b. modify the corresponding java file (`/CellGraph/src/plugins/davhelle/cellgraph/overlays/TestOverlay.java`), 
		e.g. line 52 from Color.green to Color.red
	c. return to icy, the centroids should now appear red
	
	
(*) specifically eclipse should find the dependencies in the repository:

* `CellGraph/.project`
* `CellGraph/.classpath`


# Authors
---

* Davide Heller - design & implementation
* [EpiTools team](CellGraph/License.txt) - design


# License
---
CellGraph is distributed under the GPLv3 [license](CellGraph/License.txt)


# Support
---

* In case of bugs or improvement for CellGraph feel free to:
	* Create an Issue in this code repository
	* Write to [Davide Heller](mailto:davide.heller@imls.uzh.ch?Subject=EpiTools)

# Acknowledgements
---

* The Basler Lab and von Mering Lab at IMLS for invaluable feedback
* The Yanlan Mao Lab at UCL for invaluable feedback and extensive testing 
* The icy core developers
