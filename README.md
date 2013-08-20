This project is an experimental android app that is designed to be able to locate an android device in a mapped space (in my case my condo), using wifi signals.  The program takes measurements, and you record where teh measurements are taken on the map.  From there, the app can infer locations based on other measurements in the same space.

The /python directory contains the actual models used in their simpler, easier to work with form using numpy/scipy/matplotlib.  The same model is then encoded after perfecting it into the android app in java.
