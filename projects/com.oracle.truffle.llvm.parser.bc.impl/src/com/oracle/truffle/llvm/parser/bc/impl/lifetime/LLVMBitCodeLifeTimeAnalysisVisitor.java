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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.llvm.parser.bc.impl.LLVMBitCodeReadVisitor;
import com.oracle.truffle.llvm.parser.bc.impl.LLVMPhiManager.Phi;

import uk.ac.man.cs.llvm.ir.model.FunctionDefinition;
import uk.ac.man.cs.llvm.ir.model.InstructionBlock;
import uk.ac.man.cs.llvm.ir.model.elements.BranchInstruction;
import uk.ac.man.cs.llvm.ir.model.elements.ConditionalBranchInstruction;
import uk.ac.man.cs.llvm.ir.model.elements.IndirectBranchInstruction;
import uk.ac.man.cs.llvm.ir.model.elements.Instruction;
import uk.ac.man.cs.llvm.ir.model.elements.ReturnInstruction;
import uk.ac.man.cs.llvm.ir.model.elements.SwitchInstruction;
import uk.ac.man.cs.llvm.ir.model.elements.SwitchOldInstruction;
import uk.ac.man.cs.llvm.ir.model.elements.TerminatorInstruction;
import uk.ac.man.cs.llvm.ir.model.elements.UnreachableInstruction;
import uk.ac.man.cs.llvm.ir.model.elements.ValueInstruction;

public final class LLVMBitCodeLifeTimeAnalysisVisitor {

    private final FrameDescriptor descriptor;
    private final List<InstructionBlock> blocks;
    private final Map<InstructionBlock, List<Phi>> phis;

    private Map<Instruction, List<FrameSlot>> instructionReads = new HashMap<>();
    private final Map<Instruction, List<InstructionBlock>> successorBlocks = new HashMap<>();
    private Map<Instruction, Set<FrameSlot>> blockEndKills = new HashMap<>();
    private Map<InstructionBlock, Set<FrameSlot>> blockBeginKills = new HashMap<>();
    private final Map<Instruction, Set<FrameSlot>> ins = new HashMap<>();
    private final Map<Instruction, Set<FrameSlot>> outs = new HashMap<>();
    private final Map<Instruction, Set<FrameSlot>> defs = new HashMap<>();

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
        initializeInstructionReads();

        // find ins and outs
        initializeInstructionInsOuts();

        // find definitions
        initializeDefinitions();

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

    private void initializeInstructionReads() {
        for (InstructionBlock block : blocks) {
            for (Instruction instruction : block.getInstructions()) {
                if (instruction instanceof TerminatorInstruction) {
                    successorBlocks.put(instruction, getSuccessorBlocks(instruction));
                }
                List<FrameSlot> currentInstructionReads = new LLVMBitCodeReadVisitor().getReads(instruction, descriptor);
                instructionReads.put(instruction, currentInstructionReads);
            }
        }
    }

    private List<InstructionBlock> getSuccessorBlocks(Instruction instruction) {
        List<InstructionBlock> succBlocks = new ArrayList<>();
        if (instruction instanceof BranchInstruction) {
            BranchInstruction branchInstruction = (BranchInstruction) instruction;
            succBlocks.add(branchInstruction.getSuccessor());
        } else if (instruction instanceof ConditionalBranchInstruction) {
            ConditionalBranchInstruction condBranchInstruction = (ConditionalBranchInstruction) instruction;
            if (condBranchInstruction.getTrueSuccessor() != null) {
                succBlocks.add(condBranchInstruction.getTrueSuccessor());
            }
            if (condBranchInstruction.getFalseSuccessor() != null) {
                succBlocks.add(condBranchInstruction.getFalseSuccessor());
            }
        } else if (instruction instanceof IndirectBranchInstruction) {
            IndirectBranchInstruction indirectBranchInstruction = (IndirectBranchInstruction) instruction;
            succBlocks.addAll(indirectBranchInstruction.getSuccessorBlocks());
        } else if (instruction instanceof SwitchOldInstruction) {
            SwitchOldInstruction switchOldInstruction = (SwitchOldInstruction) instruction;
            succBlocks.addAll(switchOldInstruction.getCaseBlocks());
        } else if (instruction instanceof SwitchInstruction) {
            SwitchInstruction switchInstruction = (SwitchInstruction) instruction;
            succBlocks.addAll(switchInstruction.getCaseBlocks());
        } else if (instruction instanceof ReturnInstruction || instruction instanceof UnreachableInstruction) {
            // No successor blocks so skip them
        } else {
            String msg = "Unhandled type of TerminatorInstruction";
            throw new UnsupportedOperationException(msg);
        }
        return succBlocks;
    }

    private void initializeInstructionInsOuts() {

        for (InstructionBlock block : blocks) {
            blockBeginKills.put(block, new HashSet<>());
            Instruction lastInstruction = null;
            for (Instruction instruction : block.getInstructions()) {
                lastInstruction = instruction;
                blockEndKills.put(instruction, new HashSet<>());

                Set<FrameSlot> uses = new HashSet<>(instructionReads.get(instruction));

                ins.put(instruction, uses);
                outs.put(instruction, new HashSet<>());
            }

            if (lastInstruction != null && phis.get(block) != null) {
                // Process phi instructions
                Set<FrameSlot> lastInstructionUses = ins.get(lastInstruction);
                for (Phi phi : phis.get(block)) {
                    if (phi.getValue() != null) {
                        // Need to process phi instruction, adding variables read in local
                        // TODO: Remove null to the value being read in the phi instruction
                        lastInstructionUses.add(null);
                        throw new UnsupportedOperationException();
                    }
                }
            }
        }
    }

    private void initializeDefinitions() {
        for (InstructionBlock block : blocks) {
            for (Instruction instruction : block.getInstructions()) {
                Set<FrameSlot> instructionDefs = new HashSet<>();
                if (instruction instanceof ValueInstruction) {
                    String name = ((ValueInstruction) instruction).getName();
                    instructionDefs.add(descriptor.findOrAddFrameSlot(name));
                } else {
                    // No variable defition occurred for the instruction
                }
                defs.put(instruction, instructionDefs);
            }
        }
    }
}