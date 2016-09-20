package com.oracle.truffle.llvm.parser.bc.impl;

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.llvm.parser.bc.impl.LLVMBitcodeHelper;

import uk.ac.man.cs.llvm.ir.model.FunctionParameter;
import uk.ac.man.cs.llvm.ir.model.GlobalValueSymbol;
import uk.ac.man.cs.llvm.ir.model.Symbol;
import uk.ac.man.cs.llvm.ir.model.ValueSymbol;
import uk.ac.man.cs.llvm.ir.model.constants.Constant;
import uk.ac.man.cs.llvm.ir.model.elements.AllocateInstruction;
import uk.ac.man.cs.llvm.ir.model.elements.BinaryOperationInstruction;
import uk.ac.man.cs.llvm.ir.model.elements.BranchInstruction;
import uk.ac.man.cs.llvm.ir.model.elements.CallInstruction;
import uk.ac.man.cs.llvm.ir.model.elements.CastInstruction;
import uk.ac.man.cs.llvm.ir.model.elements.CompareInstruction;
import uk.ac.man.cs.llvm.ir.model.elements.ConditionalBranchInstruction;
import uk.ac.man.cs.llvm.ir.model.elements.ExtractElementInstruction;
import uk.ac.man.cs.llvm.ir.model.elements.GetElementPointerInstruction;
import uk.ac.man.cs.llvm.ir.model.elements.IndirectBranchInstruction;
import uk.ac.man.cs.llvm.ir.model.elements.InsertElementInstruction;
import uk.ac.man.cs.llvm.ir.model.elements.InsertValueInstruction;
import uk.ac.man.cs.llvm.ir.model.elements.Instruction;
import uk.ac.man.cs.llvm.ir.model.elements.LoadInstruction;
import uk.ac.man.cs.llvm.ir.model.elements.PhiInstruction;
import uk.ac.man.cs.llvm.ir.model.elements.ReturnInstruction;
import uk.ac.man.cs.llvm.ir.model.elements.SelectInstruction;
import uk.ac.man.cs.llvm.ir.model.elements.ShuffleVectorInstruction;
import uk.ac.man.cs.llvm.ir.model.elements.StoreInstruction;
import uk.ac.man.cs.llvm.ir.model.elements.SwitchInstruction;
import uk.ac.man.cs.llvm.ir.model.elements.SwitchOldInstruction;
import uk.ac.man.cs.llvm.ir.model.elements.UnreachableInstruction;
import uk.ac.man.cs.llvm.ir.model.elements.ValueInstruction;
import uk.ac.man.cs.llvm.ir.model.elements.VoidCallInstruction;

public class LLVMBitCodeReadVisitor {

    private final List<FrameSlot> reads = new ArrayList<>();
    private FrameDescriptor descriptor;

    public List<FrameSlot> getReads(Instruction instruction, FrameDescriptor frameDescriptor) {

        this.descriptor = frameDescriptor;
        if (instruction instanceof AllocateInstruction) {
            visitAllocateInstruction((AllocateInstruction) instruction);
        } else if (instruction instanceof StoreInstruction) {
            visitStoreInstruction((StoreInstruction) instruction);
        } else if (instruction instanceof ReturnInstruction) {
            visitReturnInstruction((ReturnInstruction) instruction);
        } else if (instruction instanceof LoadInstruction) {
            visitLoadInstruction((LoadInstruction) instruction);
        } else if (instruction instanceof BinaryOperationInstruction) {
            visitBinaryOperatorInstruction((BinaryOperationInstruction) instruction);
        } else if (instruction instanceof CallInstruction) {
            visitCallInstruction((CallInstruction) instruction);
        } else if (instruction instanceof CastInstruction) {
            visitCastInstruction((CastInstruction) instruction);
        } else if (instruction instanceof CompareInstruction) {
            visitCompareInstruction((CompareInstruction) instruction);
        } else if (instruction instanceof ConditionalBranchInstruction) {
            visitConditionalBranchInstruction((ConditionalBranchInstruction) instruction);
        } else if (instruction instanceof BranchInstruction) {
            // There is no read performed on unconditional branch so skip it
        } else if (instruction instanceof IndirectBranchInstruction) {
            visitIndirectBranchInstruction((IndirectBranchInstruction) instruction);
        } else if (instruction instanceof ExtractElementInstruction) {
            visitExtractElementInstruction((ExtractElementInstruction) instruction);
        } else if (instruction instanceof GetElementPointerInstruction) {
            visitGetElementPointerInstruction((GetElementPointerInstruction) instruction);
        } else if (instruction instanceof InsertElementInstruction) {
            visitInsertElementInstruction((InsertElementInstruction) instruction);
        } else if (instruction instanceof InsertValueInstruction) {
            visitInsertValueInstruction((InsertValueInstruction) instruction);
        } else if (instruction instanceof PhiInstruction) {
            // Skip the instruction, no reads performed
        } else if (instruction instanceof SelectInstruction) {
            visitSelectInstruction((SelectInstruction) instruction);
        } else if (instruction instanceof ShuffleVectorInstruction) {
            visitShuffleVectorInstruction((ShuffleVectorInstruction) instruction);
        } else if (instruction instanceof SwitchInstruction) {
            visitSwitchInstruction((SwitchInstruction) instruction);
        } else if (instruction instanceof SwitchOldInstruction) {
            visitSwitchOldInstruction((SwitchOldInstruction) instruction);
        } else if (instruction instanceof UnreachableInstruction) {
            // Skip the instruction, no reads performed
        } else if (instruction instanceof VoidCallInstruction) {
            visitVoidCallInstruction((VoidCallInstruction) instruction);
        } else {
            String msg = "No read method imlemented for instruction type - " + instruction.getClass().getSimpleName();
            System.out.println(msg);
            throw new UnsupportedOperationException(msg);
        }
        return reads;
    }

    private void visitVoidCallInstruction(VoidCallInstruction instruction) {
        for (Symbol argument : instruction.getArguments()) {
            extractReads(argument);
        }
    }

    private void visitSwitchOldInstruction(SwitchOldInstruction instruction) {
        extractReads(instruction.getCondition());
    }

    private void visitSwitchInstruction(SwitchInstruction instruction) {
        extractReads(instruction.getCondition());
    }

    private void visitShuffleVectorInstruction(ShuffleVectorInstruction instruction) {
        extractReads(instruction.getMask());
        extractReads(instruction.getVector1());
        extractReads(instruction.getVector2());
    }

    private void visitSelectInstruction(SelectInstruction instruction) {
        extractReads(instruction.getCondition());
        extractReads(instruction.getTrueValue());
        extractReads(instruction.getFalseValue());
    }

    private void visitInsertValueInstruction(InsertValueInstruction instruction) {
        extractReads(instruction.getAggregate());
        extractReads(instruction.getValue());
    }

    private void visitInsertElementInstruction(InsertElementInstruction instruction) {
        extractReads(instruction.getIndex());
        extractReads(instruction.getVector());
        extractReads(instruction.getValue());
    }

    private void visitGetElementPointerInstruction(GetElementPointerInstruction instruction) {
        extractReads(instruction.getBasePointer());
        for (Symbol index : instruction.getIndices()) {
            extractReads(index);
        }
    }

    private void visitExtractElementInstruction(ExtractElementInstruction instruction) {
        extractReads(instruction.getIndex());
        extractReads(instruction.getVector());
    }

    private void visitIndirectBranchInstruction(IndirectBranchInstruction instruction) {

        extractReads(instruction.getAddress());
    }

    private void visitConditionalBranchInstruction(ConditionalBranchInstruction instruction) {
        extractReads(instruction.getCondition());
    }

    private void visitCompareInstruction(CompareInstruction instruction) {
        extractReads(instruction.getLHS());
        extractReads(instruction.getRHS());
    }

    private void visitCastInstruction(CastInstruction instruction) {
        extractReads(instruction.getValue());
    }

    private void visitCallInstruction(CallInstruction instruction) {
        for (Symbol argument : instruction.getArguments()) {
            extractReads(argument);
        }
    }

    private void visitBinaryOperatorInstruction(BinaryOperationInstruction binaryOperationInstruction) {
        extractReads(binaryOperationInstruction.getLHS());
        extractReads(binaryOperationInstruction.getRHS());
    }

    private void visitLoadInstruction(LoadInstruction loadInstruction) {
        extractReads(loadInstruction.getSource());
    }

    private void visitAllocateInstruction(AllocateInstruction allocateInstruction) {
        extractReads(allocateInstruction.getCount());
    }

    private void visitStoreInstruction(StoreInstruction storeInstruction) {
        extractReads(storeInstruction.getSource());
        extractReads(storeInstruction.getDestination());
    }

    private void visitReturnInstruction(ReturnInstruction returnInstruction) {
        extractReads(returnInstruction.getValue());
    }

    private void extractReads(Symbol value) {
        if (value == null)
            return;

        if (value instanceof GlobalValueSymbol || value instanceof Constant) {
            // No reads occurred - Do nothing
        } else if (value instanceof ValueInstruction || value instanceof FunctionParameter) {
            FrameSlot slot = descriptor.findOrAddFrameSlot(((ValueSymbol) value).getName());
            if (slot.getKind() == FrameSlotKind.Illegal) {
                slot.setKind(LLVMBitcodeHelper.toFrameSlotKind(value.getType()));
            }
            reads.add(slot);
        } else {
            String message = "Need to extract value from symbol of class " + value.getClass().getName();
            System.out.println(message);
            throw new UnsupportedOperationException(message);
        }
    }

}
