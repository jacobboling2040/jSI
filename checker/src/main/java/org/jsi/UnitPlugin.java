package org.jsi;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;

public class UnitPlugin implements Plugin {

  @Override
  public String getName() {
    return "jSI";
  }

  @Override
  public void init(JavacTask task, String... args) {
    Trees trees = Trees.instance(task);

    Set<CompilationUnitTree> scanned = 
      Collections.newSetFromMap(new IdentityHashMap<>()); //gaurd to prevent reporting erros twice in one file

    task.addTaskListener(new TaskListener() { //registers callback
        @Override
        public void finished(TaskEvent e) { // analyze started the trees are not atributted yet
          if (e.getKind() != TaskEvent.Kind.ANALYZE) return; // kind gard without would scan unattrirbuted trees

          CompilationUnitTree unit = e.getCompilationUnit();
          if (unit == null || !scanned.add(unit)) return;

          new UnitTreeScanner(trees, unit).scan(new TreePath(unit), null);
        }
      
    });
  }

  @Override
  public boolean autoStart() {
      return true;
  }
  
}
