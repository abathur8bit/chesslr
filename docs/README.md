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

# Power consumption
Display pulls between 450 and 470mA.

# Display rotation
Normal rotation is power cord at the bottom when it is plugged into the display board.

    pi@fireant:~ $ sudo apt-get install xinput
    pi@fireant:~ $ xinput --list
    ⎡ Virtual core pointer                    	id=2	[master pointer  (3)]
    ⎜   ↳ Virtual core XTEST pointer              	id=4	[slave  pointer  (2)]
    ⎜   ↳   Mini Keyboard                         	id=7	[slave  pointer  (2)]
    ⎜   ↳ FT5406 memory based driver              	id=8	[slave  pointer  (2)]
    ⎣ Virtual core keyboard                   	id=3	[master keyboard (2)]
        ↳ Virtual core XTEST keyboard             	id=5	[slave  keyboard (3)]
        ↳   Mini Keyboard                         	id=6	[slave  keyboard (3)]
        

## Fireant 90 degree rotation 
Power cord will be on the right side
 
    sudo vim /boot/config.txt

Add to top 

    display_rotate=1

Then run the following. `export` is only needed if you are ssh'ed in:

    export DISPLAY=:0
    xinput set-prop 'FT5406 memory based driver' 'Evdev Axes Swap' 1
    xinput --set-prop 'FT5406 memory based driver' 'Evdev Axis Inversion' 0 1
    
## Fireant 270 degree rotation
Power cord will be on the left side

    sudo vim /boot/config.txt

Add to top

    display_rotate=3

Then run the following. `export` is only needed if you are ssh'ed in:

    export DISPLAY=:0
    xinput set-prop 'FT5406 memory based driver' 'Evdev Axes Swap' 1
    xinput --set-prop 'FT5406 memory based driver' 'Evdev Axis Inversion' 1 0


## Other docs
<https://www.raspberrypi.org/documentation/hardware/display/>

Doesn't work on older Raspbain.

display_lcd_rotate=x, where x can be one of the folllowing:

    0	no rotation
    1	rotate 90 degrees clockwise
    2	rotate 180 degrees clockwise
    3	rotate 270 degrees clockwise
    0x10000	horizontal flip
    0x20000	vertical flip

display_rotate=0 Normal
display_rotate=1 90 degrees (power at right)
display_rotate=2 180 degrees
NOTE: You can rotate both the image and touch interface 180º by entering lcd_rotate=2 instead
display_rotate=3 270 degrees
display_rotate=0x10000 horizontal flip
display_rotate=0x20000 vertical flip


## Older version of Raspbian (fireant)
lcd_rotate only works with 0 and 2. 2 is flipped 180 degrees (power at the top).

If you have the lcd_rotate=2 the power cord is at the top of the display. If you don't have it, the power cord is at the bottom of the display.

No present   power cord is at bottom
lcd_rotate=0 power cord is at bottom
lcd_rotate=1 power cord is at bottom
lcd_rotate=2 power cord is at top
lcd_rotate=3 power cord is at bottom


    sudo vim /boot/config.txt
    
    # screen totation added by me
    #lcd_rotate=2
    #display_rotate=1
    
