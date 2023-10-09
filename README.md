[![.github/workflows/ant.yml](https://github.com/Funz/plugin-Cathare/actions/workflows/ant.yml/badge.svg)](https://github.com/Funz/plugin-Cathare/actions/workflows/ant.yml)

# Funz plugin: Cathare

This plugin is dedicated to launch Cathare calculations from Funz.
It supports the following syntax and features:

  * Input
    * main file
    * PERMINIT file (if available)
    * PERMINIT postpro file (if available)
    * postpro file
    * reader mask files (if available)
    * parameter syntax: 
      * variable syntax: `$(...)`
      * formula syntax: `@{...}`
      * comment char: `*`
    * example input files:
      * main file
        ```
        REALC BOTTOM  P     $(p~57.)D5
                      TL    $(tl~[232.,242.])
                      HVSAT
                      ALFA  1.0D-5
                      VL    1.0D-5
                      VV    1.0D-5;
        ```      
      * will identify input:
        * p, expected to default value 57.0
        * tl, expected to vary inside [232,242]
  * Output
    * file type supported: Cathare FORT07 file



![Analytics](https://ga-beacon.appspot.com/UA-109580-20/plugin-Cathare)
