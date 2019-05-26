package App;

import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.util.LinkedList;

import Miscellaneous.Notification;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;


public class Compiler
{
    private Notification noti = new Notification();

    private static String IP;


    public Compiler( String ip )
    {
        IP = ip;
    }


    /**
     * 
     * TODO Write your method description here.
     * 
     * @param list
     * @param workspace
     * @param stage
     * @param byt
     *            0 = save; 1 = open
     */
    public void send( LinkedList<Node> list, Pane workspace, final Stage stage )
    {
        try
        {
            FileChooser fileChoose = new FileChooser();
            File file = fileChoose.showSaveDialog( stage );

            if ( file == null )
            {
                noti.saveCancel();
            }
            else if ( file.getName().length() < 5 || !file.getName()
                .substring( file.getName().length() - 5 )
                .equals( ".java" ) )
            {
                noti.saveFail();
            }
            else
            {
                Socket socket = new Socket( IP, 6666 );
                ObjectOutputStream oos = new ObjectOutputStream(
                    socket.getOutputStream() );
                ObjectInputStream ois = new ObjectInputStream(
                    socket.getInputStream() );

                @SuppressWarnings("resource")
                PrintWriter out = new PrintWriter(
                    new BufferedWriter( new FileWriter( file ) ) );

                Thread putOut = new Thread()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            oos.writeByte( 0 );
                            oos.writeObject( workspace.getWidth() + ", "
                                + workspace.getHeight() );
                            for ( Node n : list )
                            {
                                Bounds boundsInScene = n
                                    .localToParent( n.getLayoutBounds() );
                                if ( n instanceof Label )
                                {
                                    sendLabel( oos, boundsInScene, n );
                                }
                                else if ( n instanceof Button )
                                {
                                    sendButton( oos, boundsInScene, n );
                                }
                                else if ( n instanceof TextInputControl )
                                {
                                    sendTextField( oos, boundsInScene, n );
                                }
                            }
                            oos.writeObject( "quit" );
                        }
                        catch ( Exception ex )
                        {
                            System.out.println( "heheo " + ex );
                        }
                    }
                };

                Thread takeIn = new Thread()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            String meme = (String)ois.readObject();
                            while ( !meme.equals( "quit" ) )
                            {
                                out.println( meme );
                                meme = (String)ois.readObject();
                            }
                        }
                        catch ( EOFException ex )
                        {
                            out.close();
                            try
                            {
                                socket.close();
                            }
                            catch ( IOException e )
                            {
                                e.printStackTrace();
                            }
                        }
                        catch ( Exception ex )
                        {
                            System.out.println( "hehei " + ex );
                        }
                    }
                };

                putOut.setDaemon( true );
                takeIn.setDaemon( true );

                takeIn.start();
                putOut.start();

                noti.saveSuccess();
            }
        }
        catch ( ConnectException ex )
        {
            noti.connectionFail();
        }
        catch ( Exception ex )
        {
            System.out.println( ex );
        }
    }


    public void open( final Stage stage )
    {

        FileChooser fileChoose = new FileChooser();
        File file = fileChoose.showSaveDialog( stage );

        if ( file == null )
        {
            noti.saveCancel();
        }
        else if ( file.getName().length() < 5 || !file.getName()
            .substring( file.getName().length() - 5 )
            .equals( ".java" ) )
        {
            noti.saveFail();
        }
        else
        {
            try
            {
                Socket socket = new Socket( IP, 6666 );
                ObjectOutputStream oos = new ObjectOutputStream(
                    socket.getOutputStream() );
                ObjectInputStream ois = new ObjectInputStream(
                    socket.getInputStream() );

                @SuppressWarnings("resource")
                PrintWriter out = new PrintWriter(
                    new BufferedWriter( new FileWriter( file ) ) );

                oos.writeByte( 1 );
                oos.flush();
                String fileName = openPane( stage, ois );
                System.out.println( fileName );
                socket.close();
                retrieve( fileName, stage, file );
            }

            catch ( Exception ex )
            {
                System.out.println( ex );
            }
        }
    }


    private void retrieve( String fileName, final Stage stage, File file )
    {
        try
        {
            Socket socket = new Socket( IP, 6666 );
            ObjectOutputStream oos = new ObjectOutputStream(
                socket.getOutputStream() );
            ObjectInputStream ois = new ObjectInputStream(
                socket.getInputStream() );

            PrintWriter out = new PrintWriter(
                new BufferedWriter( new FileWriter( file ) ) );

            oos.writeByte( 2 );
            oos.writeObject( fileName );
            oos.flush();

            try
            {
                String meme = (String)ois.readObject();
                while ( !meme.equals( "quit" ) )
                {
                    out.println( meme );
                    meme = (String)ois.readObject();
                }
            }
            catch ( EOFException ex )
            {
                out.close();
                try
                {
                    socket.close();
                }
                catch ( IOException e )
                {
                    e.printStackTrace();
                }
            }
            catch ( Exception ex )
            {
                System.out.println( "hehei " + ex );
            }

            out.close();
            socket.close();
        }
        catch ( Exception ex )
        {
            System.out.println( ex );
        }

    }


    private void sendButton(
        ObjectOutputStream oos,
        Bounds boundsInScene,
        Node n )
        throws Exception
    {
        oos.writeObject( "Labeled" );
        oos.writeObject( ( (Labeled)n ).getText() );
        oos.writeObject( Double.toString( boundsInScene.getMinX() ) );
        oos.writeObject( Double.toString( boundsInScene.getMinY() ) );
        oos.writeObject( Double.toString( boundsInScene.getWidth() ) );
        oos.writeObject( Double.toString( boundsInScene.getHeight() ) );
    }


    private void sendLabel(
        ObjectOutputStream oos,
        Bounds boundsInScene,
        Node n )
        throws Exception
    {
        oos.writeObject( "Labeled" );
        oos.writeObject( ( (Labeled)n ).getText() );
        oos.writeObject( Double.toString( boundsInScene.getMinX() ) );
        oos.writeObject( Double.toString( boundsInScene.getMinY() ) );
        oos.writeObject( Double.toString( boundsInScene.getWidth() ) );
        oos.writeObject( Double.toString( boundsInScene.getHeight() ) );
    }


    private void sendTextField(
        ObjectOutputStream oos,
        Bounds boundsInScene,
        Node n )
        throws Exception
    {
        oos.writeObject( "TextField" );
        oos.writeObject( ( (TextInputControl)n ).getText() );
        oos.writeObject( Double.toString( boundsInScene.getMinX() ) );
        oos.writeObject( Double.toString( boundsInScene.getMinY() ) );
        oos.writeObject( Double.toString( boundsInScene.getWidth() ) );
        oos.writeObject( Double.toString( boundsInScene.getHeight() ) );
    }


    private String openPane( Stage stage, ObjectInputStream ois )
    {
        final Stage popup = new Stage();
        popup.setTitle( "Backups" );
        popup.setResizable( false );
        popup.initModality( Modality.APPLICATION_MODAL );
        popup.initOwner( stage );
        VBox vbox = new VBox();
        vbox.setAlignment( Pos.CENTER );
        vbox.setPadding( new Insets( 10, 10, 10, 10 ) );
        vbox.setSpacing( 10 );

        try
        {
            int fileMapSize = ois.readInt();
            System.out.println( "File Map Size " + fileMapSize );

        }
        catch ( IOException e1 )
        {
            e1.printStackTrace();
        }

        ToggleGroup toggleGroup = new ToggleGroup();

        while ( true )
        {
            try
            {
                String str = (String)ois.readObject();
                RadioButton meme = new RadioButton( str );
                meme.setToggleGroup( toggleGroup );
                vbox.getChildren().add( meme );
                toggleGroup.selectToggle( meme );
            }
            catch ( EOFException ex )
            {
                break;
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
        }

        Button ass = new Button( "Submit" );
        vbox.getChildren().add( ass );
        ass.setOnAction( e -> {
            popup.close();
        } );

        Scene scene = new Scene( vbox );
        popup.setScene( scene );
        popup.showAndWait();
        RadioButton out = (RadioButton)toggleGroup.getSelectedToggle();
        return out.getText();
    }
}
