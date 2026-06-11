package codes.reason.murdertils;


import java.util.function.Function;

import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

// https://easings.net/
// https://gist.github.com/dev-hydrogen/21a66f83f0386123e0c0acf107254843
public enum Easing {

    EASE_IN_SINE(x -> 1 - cos(x * PI) / 2),
    EASE_OUT_SINE(x -> sin(x * PI) / 2),
    EASE_IN_OUT_SINE(x -> -(cos(PI * x) - 1) / 2),
    EASE_IN_QUAD(x -> x * x),
    EASE_OUT_QUAD(x -> 1 - (1 - x) * (1 - x)),
    EASE_IN_OUT_QUAD(x -> x < 0.5 ? 2 * x * x : 1 - pow(-2 * x + 2, 2) / 2),
    EASE_IN_CUBIC(x -> x * x * x),
    EASE_OUT_CUBIC(x -> 1 - pow(1 - x, 3)),
    EASE_IN_OUT_CUBIC(x -> x < 0.5 ? 4 * x * x * x : 1 - pow(-2 * x + 2, 3) / 2),
    EASE_IN_QUART(x -> x * x * x * x),
    EASE_OUT_QUART(x -> 1 - pow(1 - x, 4)),
    EASE_IN_OUT_QUART(x -> x < 0.5 ? 8 * x * x * x * x : 1 - pow(-2 * x + 2, 4) / 2),
    EASE_IN_QUINT(x -> x * x * x * x * x),
    EASE_OUT_QUINT(x -> 1 - pow(1 - x, 5)),
    EASE_IN_OUT_QUINT(x -> x < 0.5 ? 16 * x * x * x * x * x : 1 - pow(-2 * x + 2, 5) / 2),
    EASE_IN_EXPO(x -> x == 0 ? 0 : pow(2, 10 * x - 10)),
    EASE_OUT_EXPO(x -> x == 1 ? 1 : 1 - pow(2, -10 * x)),
    EASE_IN_OUT_EXPO(x -> x == 0 ? 0 : x == 1 ? 1 : x < 0.5 ? pow(2, 20 * x - 10) / 2 : (2 - pow(2, -20 * x + 10)) / 2),
    EASE_IN_CIRC(x -> 1 - sqrt(1 - pow(x, 2))),
    EASE_OUT_CIRC(x -> sqrt(1 - pow(x - 1, 2))),
    EASE_IN_OUT_CIRC(x -> x < 0.5 ? (1 - sqrt(1 - pow(2 * x, 2))) / 2 : (sqrt(1 - pow(-2 * x + 2, 2)) + 1) / 2),
    EASE_IN_BACK(x -> 2.70158 * x * x * x - 1.70158 * x * x),
    EASE_OUT_BACK(x -> 1 + 2.70158 * pow(x - 1, 3) + 1.70158 * pow(x - 1, 2)),
    EASE_IN_OUT_BACK(x -> x < 0.5 ? (pow(2 * x, 2) * ((1.70158 * 1.525 + 1) * 2 * x - 1.70158 * 1.525)) / 2 : (pow(2 * x - 2, 2) * ((1.70158 * 1.525 + 1) * (x * 2 - 2) + 1.70158 * 1.525) + 2) / 2),
    EASE_IN_ELASTIC(x -> x == 0 ? 0 : x == 1 ? 1 : -pow(2, 10 * x - 10) * sin((x * 10 - 10.75) * ((2 * PI) / 3))),
    EASE_OUT_ELASTIC(x -> x == 0 ? 0 : x == 1 ? 1 : pow(2, -10 * x) * sin((x * 10 - 0.75) * ((2 * PI) / 3)) + 1),
    EASE_IN_OUT_ELASTIC(x -> x == 0 ? 0 : x == 1 ? 1 : x < 0.5 ? -(pow(2, 20 * x - 10) * sin((20 * x - 11.125) * ((2 * PI) / 4.5))) / 2 : (pow(2, -20 * x + 10) * sin((20 * x - 11.125) * ((2 * PI) / 4.5))) / 2 + 1);

    private final Function<Double, Number> function;

    Easing(Function<Double, Number> function) {
        this.function = function;
    }

    public Function<Double, Number> getFunction() {
        return function;
    }

}
