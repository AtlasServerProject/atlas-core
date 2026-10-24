package io.atlas.modules.ranked.repository;
import com.google.gson.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.util.*;
public final class RankedResultJournal {
 public record Result(UUID match,UUID battle,UUID winner,UUID loser){}
 private final Path root;
 public RankedResultJournal(Path root){this.root=root;}
 public void save(Result result){try{
  Files.createDirectories(root);Files.setPosixFilePermissions(root,PosixFilePermissions.fromString("rwx------"));
  Path temp=Files.createTempFile(root,"pending-",".tmp",PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
  try{
   byte[] bytes=new Gson().toJson(result).getBytes(java.nio.charset.StandardCharsets.UTF_8);
   try(var channel=FileChannel.open(temp,StandardOpenOption.WRITE)){ByteBuffer buffer=ByteBuffer.wrap(bytes);while(buffer.hasRemaining())channel.write(buffer);channel.force(true);}
   Files.move(temp,root.resolve(result.match()+".json"),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
   try(var directory=FileChannel.open(root,StandardOpenOption.READ)){directory.force(true);}
  }finally{Files.deleteIfExists(temp);}
 }catch(java.io.IOException e){throw new IllegalStateException("Falha ao persistir resultado ranqueado",e);}}
 public List<Result> pending(){try{if(!Files.isDirectory(root))return List.of();var out=new ArrayList<Result>();try(var paths=Files.list(root)){for(Path path:paths.filter(p->p.toString().endsWith(".json")).sorted().toList())out.add(new Gson().fromJson(Files.readString(path),Result.class));}return out;}catch(java.io.IOException e){throw new IllegalStateException(e);}}
 public void done(UUID id){try{Files.deleteIfExists(root.resolve(id+".json"));}catch(java.io.IOException e){throw new IllegalStateException(e);}}
}
