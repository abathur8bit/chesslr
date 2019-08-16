Contains any reference material used for the project. This includes anything related to ChessLR game or electronics of the board itself. 

# Running remotely

lee@titan: $ ssh pi@botfly
pi@fireant:~ $ cd chesslr
pi@fireant:~/chesslr $ export DISPLAY=:0
pi@fireant:~/chesslr $ java -cp chesslr-A.1-jar-with-dependencies.jar com.axorion.chesslr.ChessLR

# MCP23017

Pin layout

```
          +----- -----+
GPB0 <--> |*1   -   28| <--> GPA7
GPB1 <--> |2        27| <--> GPA6
GPB2 <--> |3        26| <--> GPA5
GPB3 <--> |4    M   25| <--> GPA4
GPB4 <--> |5    C   24| <--> GPA3
GPB5 <--> |6    P   23| <--> GPA2
GPB6 <--> |7    2   22| <--> GPA1
GPB7 <--> |8    3   21| <--> GPA0
 VDD ---> |9    0   20| ---> INTA
 VSS ---> |10   1   19| ---> INTB
  NC ---- |11   7   18| ---> RESET
 SCL ---> |12       17| <--- A2
 SDA <--> |13       16| <--- A1
  NC ---- |14       15| <--- A0
          +-----------+
```
