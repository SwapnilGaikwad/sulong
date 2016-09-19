/*
 * Copyright (c) 2016, Oracle and/or its affiliates.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.oracle.truffle.llvm.parser.bc.impl.lifetime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.llvm.parser.bc.impl.LLVMPhiManager.Phi;

import uk.ac.man.cs.llvm.ir.model.FunctionDefinition;
import uk.ac.man.cs.llvm.ir.model.InstructionBlock;

public final class LLVMBitCodeLifeTimeAnalysisVisitor {

    private final FrameDescriptor descriptor;
    private final List<InstructionBlock> blocks;
    private final Map<InstructionBlock, List<Phi>> phis;

    private LLVMBitCodeLifeTimeAnalysisVisitor(FunctionDefinition definition, FrameDescriptor descriptor, Map<InstructionBlock, List<Phi>> phis) {

        this.descriptor = descriptor;
        this.blocks = definition.getBlocks();
        this.phis = phis;
    }

    public static LLVMBitcodeLifeTimeAnalysisResult visit(FunctionDefinition definition, FrameDescriptor descriptor, Map<InstructionBlock, List<Phi>> phis) {
        LLVMBitcodeLifeTimeAnalysisResult result = new LLVMBitCodeLifeTimeAnalysisVisitor(definition, descriptor, phis).visit();
        return result;
    }

    private LLVMBitcodeLifeTimeAnalysisResult visit() {

        // find instruction reads

        // find ins and outs

        // find definitions

        // perform data flow analysis algorithm

        // find instruction kills

        // generate begin end kills and return
        Map<InstructionBlock, FrameSlot[]> beginKills = new HashMap<>();
        Map<InstructionBlock, FrameSlot[]> endKills = new HashMap<>();
        for (InstructionBlock block : blocks) {
            // Add empty slots for each instruction block
            beginKills.put(block, new FrameSlot[0]);
            endKills.put(block, new FrameSlot[0]);
        }

        return new LLVMBitcodeLifeTimeAnalysisResult(beginKills, endKills);
    }

}
