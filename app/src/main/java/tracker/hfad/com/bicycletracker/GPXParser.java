package tracker.hfad.com.bicycletracker;

import java.util.LinkedList;

public class GPXParser
{
    public static class Parameter
    {
        String name, value;

        Parameter(String name, String value)
        {
            this.name = name;
            this.value = value;
        }

        String toGPX()
        {
            String buffer=" ";

            buffer+=this.name;
            buffer+="=\"";
            buffer+=this.value;
            buffer+="\"";

            return buffer;
        }
    }

    public static class Element
    {
        private LinkedList<Element> nodes;
        private LinkedList<Parameter> parameters;
        private String name, value;

        private String generateOpenTag()
        {
            String buffer = "\n<";
            buffer+=this.name;

            for( Parameter parameter : parameters)
            {
                buffer+=parameter.toGPX();
            }

            buffer+=">";

            return  buffer;
        }

        private String generateCloseTag()
        {
            String buffer="";

            if(!this.nodes.isEmpty()) buffer+="\n";

            buffer += "</";
            buffer+=this.name;
            buffer+=">";

            return  buffer;
        }

        Element(String name, String value)
        {
            this.name = name;
            this.value = value;

            this.nodes = new LinkedList<Element>();
            this.parameters = new LinkedList<Parameter>();
        }

        void appendChild(Element child)
        {
            nodes.add(child);
        }

        void appendParameter(Parameter parameter)
        {
            parameters.add(parameter);
        }

        String toGPX()
        {
            String buffer="";

            buffer+=generateOpenTag();

            if(nodes.isEmpty()) buffer+=this.value;

            for(Element node : nodes)
            {
                buffer+=node.toGPX();
            }

            buffer+=generateCloseTag();

            return buffer;
        }
    }

}
