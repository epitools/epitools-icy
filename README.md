# Welcome to CellGraph (a.k.a. icy plugins for EpiTools)
---

CellGraph is a plugin collection for the bioimaging framework [icy](http://icy.bioimageanalysis.org) and allows to transform skeleton images into interactive overlays to explore and analyze your data (examples [here](https://epitools.github.io/wiki/Icy_Plugins/01_CellOverlay/)). CellGraph is part of the EpiTools project, an open source image analysis toolkit for quantifying epithelial growth dynamics. To know more about the project please visit our project website or the related code repositories:

* EpiTools homepage: [https://epitools.github.io](https://epitools.github.io)
* Image processing part (MATLAB): [https://github.com/epitools/epitools-matlab](https://github.com/epitools/epitools-matlab)

If you like CellGraph and use it in your work, please cite our EpiTools paper in Developmental Cell (January 2016), freely available at [http://dx.doi.org/10.1016/j.devcel.2015.12.012](http://dx.doi.org/10.1016/j.devcel.2015.12.012)

# Plugin subdivision
---
The plugins cover subsequent steps of the analysis: 

1. [CellGraph](https://epitools.github.io/wiki/Icy_Plugins/02_CellGraph/) generates the spatiotemporal graph starting from input skeleton files
2. [CellEditor](https://epitools.github.io/wiki/Icy_Plugins/04_CellEditor/) enables the user to interactively modify the skeleton images manually in case of any remaining segmentation mistakes 
3. [CellOverlay](https://epitools.github.io/wiki/Icy_Plugins/01_CellOverlay/) interprets the data and outputs results in the form of graphical overlays (i.e. additional image layers) and tabular files
4. [CellExporter](https://epitools.github.io/wiki/Icy_Plugins/03_CellExport/) allows the user to export the complete numerical data in various formats, such as Excel and GraphML
5. [CellSurface](https://epitools.github.io/wiki/Icy_Plugins/05_CellSurface/) visualizes the VTK files exported by the [surface fitting](https://epitools.github.io/wiki/Analysis_Modules/00_projection/) in Matlab 

Every plugin has a separate GUI and can be conveniently accessed through the [EpiTools toolbar]()

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

CellGraph was developed using the [method](http://icy.bioimageanalysis.org/index.php?display=startDevWithIcy) suggested by the icy developers, which is based on eclipse and allows launching the plugin directly from there.

To get started developing, we suggest editing the `TestOverlay`. Assuming you have icy and epitools already set up, follow these steps:

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
* [EpiTools team](LICENSE.txt) - design


# License
---
CellGraph is distributed under the GPLv3 [license](LICENSE.txt)


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
