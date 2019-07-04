import com.pi4j.gpio.extension.mcp.MCP23017GpioProvider;
import com.pi4j.gpio.extension.mcp.MCP23017Pin;
import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CFactory;

import java.io.IOException;

public class MCPExample {
    public static void main(String[] args) throws InterruptedException, IOException, I2CFactory.UnsupportedBusNumberException {
        int address = 0x20;
        System.out.format("<--Pi4J--> MCP23017 GPIO Example ... started using address %02X.\n",address);
        System.out.format("<--Pi4J--> MCP23017 GPIO Example ... started using address %02X.\n",address+1);

        // create gpio controller
        final GpioController gpio = GpioFactory.getInstance();

        // create custom MCP23017 GPIO provider
        final MCP23017GpioProvider pro0 = new MCP23017GpioProvider(I2CBus.BUS_1, address);
        final MCP23017GpioProvider pro1 = new MCP23017GpioProvider(I2CBus.BUS_1, address+1);

        // provision gpio input pins from MCP23017
        GpioPinDigitalInput in0[] = {
                gpio.provisionDigitalInputPin(pro0, MCP23017Pin.GPIO_B0, "in0-B0", PinPullResistance.PULL_UP),
        };

        // create and register gpio pin listener
        gpio.addListener(new GpioPinListenerDigital() {
            //            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                // display pin state on console
                System.out.println(" --> GPIO PIN STATE CHANGE: " + event.getPin() + " = " + event.getState());
            }
        }, in0);

        // provision gpio output pins and make sure they are all LOW at startup
        GpioPinDigitalOutput out0[] = {
                gpio.provisionDigitalOutputPin(pro0, MCP23017Pin.GPIO_A0, "out0-A0", PinState.LOW),
                gpio.provisionDigitalOutputPin(pro0, MCP23017Pin.GPIO_B7, "out0-B7", PinState.LOW),
        };
        GpioPinDigitalOutput out1[] = {
                gpio.provisionDigitalOutputPin(pro1,MCP23017Pin.GPIO_A1,"out1-A1",PinState.LOW),
        };
        // keep program running for 20 seconds
        for (int count = 0; count < 10; count++) {
            System.out.println("On");
            gpio.setState(true, out0);
            gpio.setState(true, out1);
            Thread.sleep(250);
            System.out.println("Off");
            gpio.setState(false, out0);
            gpio.setState(false, out1);
            Thread.sleep(250);
        }

        // stop all GPIO activity/threads by shutting down the GPIO controller
        // (this method will forcefully shutdown all GPIO monitoring threads and scheduled tasks)
        gpio.shutdown();

        System.out.println("Exiting MCP23017GpioExample");
    }
}
