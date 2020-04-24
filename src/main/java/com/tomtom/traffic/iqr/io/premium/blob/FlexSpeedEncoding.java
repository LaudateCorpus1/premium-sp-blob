/*
 * Copyright (C) 2020 TomTom N.V. (www.tomtom.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tomtom.traffic.iqr.io.premium.blob;

/**
 * Utility class providing functionality for encoding speed values as 10-bit numbers with adaptive relative precision.
 *
 * The values are encoded as 10-bit-floating-point values with 3 bits for the exponent and 7 bits for the mantissa.
 *
 * This encoding can represent 1024 values from 0 to 255. The smallest non-zero value that can be represented is 1/64.
 * The provided precision is not uniform but is different for different value ranges as shown in the following:
 *
 * <table>
 * <tr><th>Exponent    </th> <th>Range          </th> <th>Precision </th></tr>
 * <tr><td>0           </td> <td>0-2            </td> <td> 1/64 </td></tr>
 * <tr><td>1           </td> <td>2-4            </td> <td> 1/64 </td></tr>
 * <tr><td>2           </td> <td>4-8            </td> <td> 1/32 </td></tr>
 * <tr><td>3           </td> <td>8-16           </td> <td> 1/16 </td></tr>
 * <tr><td>4           </td> <td>16-32          </td> <td> 1/8  </td></tr>
 * <tr><td>5           </td> <td>32-64          </td> <td> 1/4  </td></tr>
 * <tr><td>6           </td> <td>64-128         </td> <td> 1/2  </td></tr>
 * <tr><td>7           </td> <td>128-255        </td> <td> 1    </td></tr>
 * </table>
 */

class FlexSpeedEncoding {

    private static final double LOG_2 = Math.log(2);

    private FlexSpeedEncoding() {
        // Hiding the implicit public constructor
    }

    /**
     * Encodes a given speed value as a 10-bit floating-point value.
     *
     * Values above 255 and below 0 are capped.
     *
     * @param speed  the speed value to be encoded
     * @return the 10-bit floating-point representation of the value in form of a {@code short}
     */
    public static short encode(final double speed) {
        if (speed <= 0) {
            return 0;
        }
        if (speed <= 2) {
            return (short) Math.round(speed * 64);
        }
        double encodedSpeed = speed;
        if (encodedSpeed >= 255) {
            encodedSpeed = 255;
        }
        int exponent = iLogB(encodedSpeed);
        int mantissa = (int) Math.round(scaleBN(encodedSpeed, 7 - exponent) - 128);
        return (short) ((exponent << 7) + mantissa);
    }

    /**
     * Interprets the passed {@code short} value as a 10-bit floating-point-value and returns the decoded value as {@code double}.
     *
     * @param raw  the 10-bit floating-point representation to be decoded
     * @return the decoded value as a {@code double}
     */
    public static double decode(final short raw) {
        if (raw == 0) {
            return 0.0;
        }
        int mantissa = raw & 0x7F;
        int exponent = (raw >> 7) & 7;
        if (exponent == 0) {
            return scaleBN(mantissa, -6);
        }
        return scaleBN(mantissa + 128.0, exponent - 7);
    }

    // Helper method: returns the integral part of the base-2 logarithm of the passed value
    // Modeled after ilogb in C++ (see e.g. https://en.cppreference.com/w/cpp/numeric/math/ilogb)
    private static int iLogB(final double speed) {
        return (int) Math.floor(Math.log(speed) / LOG_2);
    }

    // Helper method: scales speed by 2 to the power of exp
    // Modeled after scalbn in C++ (see e.g. https://en.cppreference.com/w/cpp/numeric/math/scalbn)
    private static double scaleBN(final double speed, int exp) {
        int absExp = Math.abs(exp);
        int factor = 1;
        for (int i = 1; i <= absExp; ++i) {
            factor *= 2;
        }
        if (exp < 0) {
            return speed / factor;
        }
        return speed * factor;
    }

}
