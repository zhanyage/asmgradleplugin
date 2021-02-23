package com.zhanyage.asm

class AsmExtension {
    boolean enable
    String output
    String baseMethodMapFile
    String blackListFile


    AsmExtension() {
        enable = true
        output = ""
        baseMethodMapFile = ""
        blackListFile = ""
    }

    @Override
    String toString() {
        """| enable = ${enable}
           | output = ${output}
        """.stripMargin()
    }
}