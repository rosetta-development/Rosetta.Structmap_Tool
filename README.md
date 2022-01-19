# Rosetta.Structmap_Tool
The tool helps to add or update logical structure maps in Rosetta. Since structure map XMLs can have very complex structures - making it sometimes difficult to adjust them - the tool transforms existing XMLs to CSVs with a simplified structure, which can therefor easier be edited by Rosetta staff users. When the customization is finished, the tool creates/updates logical structure maps in Rosetta via API.

The tool supports batch processing.

The tool is designed for Windows and runs in a dedicated frame. 

Depending on configuration and selected processing option, the tool
- requests structure maps from Rosetta via API (single or batch)
- converts XML to CSV 
- adds columns to the CSV
- extracts new file label from former file label via regular expression
- converts adjusted CSV to valid structure map XML
- adds logical structure maps via API (when created based on existing physical structure maps)
- updates logical structure maps via API (using edited existing logical structure maps) 
- has flexibility because of configuration options
- writes a log file (with debug information, when configured)

--------

The file Rosetta_CreateLogicalStructureMap_Tool.zip contains an executable jar together with the configuration file and a PDF with configuration and usage instructions.
