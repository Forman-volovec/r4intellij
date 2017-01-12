/*
 * Copyright 2011-2011 Gregory Shrago
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.r4intellij.psi;

import com.intellij.ide.structureView.*;
import com.intellij.ide.util.treeView.smartTree.Filter;
import com.intellij.ide.util.treeView.smartTree.Grouper;
import com.intellij.ide.util.treeView.smartTree.Sorter;
import com.intellij.lang.PsiStructureViewFactory;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.IconLoader;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.util.PlatformIcons;
import com.r4intellij.psi.api.RAssignmentStatement;
import com.r4intellij.psi.api.RFile;
import com.r4intellij.psi.api.RFunctionExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;


/**
 * @author gregsh
 */
public class RScriptStructureViewFactory implements PsiStructureViewFactory {

    public StructureViewBuilder getStructureViewBuilder(final PsiFile psiFile) {
        return new TreeBasedStructureViewBuilder() {

            @NotNull
            @Override
            public StructureViewModel createStructureViewModel(@Nullable Editor editor) {
                return new RStructureViewModel(psiFile);
            }


            @Override
            public boolean isRootNodeShown() {
                return false;
            }
        };
    }


    public class RStructureViewModel extends TextEditorBasedStructureViewModel {

        private PsiFile myFile;

        //todo implement meaningful filters and groupers
//        private Filter[] filters = new Filter[]{new DefFilter(), new MacFilter(), new EqFilter()};
//        private Grouper[] groupers = new Grouper[]{new DefMacGrouper()};
        private Filter[] filters = new Filter[]{};
        private Grouper[] groupers = new Grouper[]{};


        public RStructureViewModel(final PsiFile file) {
            super(file);
            myFile = file;
        }


        @NotNull
        public StructureViewTreeElement getRoot() {
            return new RStructureViewElement(myFile);
        }


        @NotNull
        public Grouper[] getGroupers() {
            return groupers;
        }


        @NotNull
        public Sorter[] getSorters() {
            // TODO - Enable sorting based on defs, macs, fns, []s, etc...
            return new Sorter[]{Sorter.ALPHA_SORTER};
        }


        @NotNull
        public Filter[] getFilters() {
            return filters;
        }


        protected PsiFile getPsiFile() {
            return myFile;
        }


        @NotNull
        protected Class[] getSuitableClasses() {
            return new Class[]{RFunctionExpression.class, PsiComment.class};
        }
    }


    public static class RStructureViewElement implements StructureViewTreeElement, ItemPresentation {

        private final PsiElement myElement;


        public RStructureViewElement(PsiElement element) {
            this.myElement = element;
        }


        @Override
        public Object getValue() {
            return myElement;
        }


        @Override
        public void navigate(boolean requestFocus) {
            ((Navigatable) myElement).navigate(requestFocus);
        }


        @Override
        public boolean canNavigate() {
            return ((Navigatable) myElement).canNavigate();
        }


        @Override
        public boolean canNavigateToSource() {
            return ((Navigatable) myElement).canNavigateToSource();
        }


        @NotNull
        @Override
        public ItemPresentation getPresentation() {
            return this;
        }


        @NotNull
        public StructureViewTreeElement[] getChildren() {

            final List<PsiElement> childrenElements = new ArrayList<PsiElement>();


            // split file into sections
            if (myElement instanceof RFile) {
                myElement.acceptChildren(new PsiElementVisitor() {

                    @Override
                    public void visitComment(PsiComment comment) {
                        if (isSectionDivider(comment)) {
                            childrenElements.add(comment);
                        }
                    }

                    //no further recursion here because we just support sectioning on top level
                });
            }

            PsiElementVisitor functionCollector = new PsiElementVisitor() {
                public void visitElement(PsiElement element) {
//                        if (element instanceof RSection || (getFunctionName(element) != null && getSection(element) == null)) {
                    if (element instanceof RFunctionExpression) {
                        childrenElements.add(element);
                        return;
                    }

                    element.acceptChildren(this);
                }
            };

            myElement.acceptChildren(functionCollector);


            // reshape into array
            StructureViewTreeElement[] children = new StructureViewTreeElement[childrenElements.size()];
            for (int i = 0; i < children.length; i++) {
                children[i] = new RStructureViewElement(childrenElements.get(i));
            }

            return children;
        }


        private boolean isSectionDivider(PsiElement myElement) {
            if (myElement instanceof PsiComment) {
                return myElement.getText().startsWith("#' #");
            }

            return false;
        }


        private PsiElement getSection(PsiElement element) {
            //note in old version of r4ij this was implemented in RCommandImpl using
            // PsiElementBase.findChildByClass(RSection.class)

            if (isSectionDivider(element.getPrevSibling())) return element.getPrevSibling();

            return null;
        }


        @Override
        public String getPresentableText() {
            if (myElement instanceof RFile) {
                return ((RFile) myElement).getName();
            }
            if (myElement instanceof RFunctionExpression) {
                if (myElement.getParent() instanceof RAssignmentStatement) {
                    return ((RAssignmentStatementImpl) myElement.getParent()).getAssignee().getText();
                } else {
                    return "anonymous function";
                }

            } else if (myElement instanceof PsiComment) {
                return myElement.getText();
            }

            throw new AssertionError(myElement.getClass().getName());
        }


        @Override
        public String getLocationString() {
            return null;
        }


        @Override
        public Icon getIcon(boolean open) {
//      return myElement instanceof RSection ? PlatformIcons.PACKAGE_ICON : myElement.getIcon(0);
            return myElement instanceof RFunctionExpression ? PlatformIcons.METHOD_ICON : (open ? IconLoader.getIcon("/nodes/folderOpen.png") : PlatformIcons.FOLDER_ICON);
        }
    }
}
