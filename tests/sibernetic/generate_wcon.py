import sys 
import math

    
def generate_wcon(pos_file_name, wcon_file_name, rate_to_plot = 1):
    
    postions_file = open(pos_file_name)
    
    max_lines = 100
    max_time_ms = 10
    
    line_num = 0

    

    print("Loading: %s"%pos_file_name)
    
    
    ts = []
    x = {}
    y = {}
    z = {}
    
    num_plotted_frames=0
    points_plotted = 0
    
    middle_points=[]
    ave_points=[]
    middle_point_speed_x = []
    middle_point_speed_y = []
    ave_point_speed_x = []
    ave_point_speed_y = []
    time_points = []
    num_frames=0
    
    for line in postions_file:

        line_num +=1
        if line_num>max_lines:
            print("Finished parsing file, as max number of lines reached!")
            break
        
        if line_num%rate_to_plot==1:
            num_frames+=1
            words = line.split()
            points = (len(words)-4)/3
            middle_point = points/2

            t_ms = float(words[0])
            ts.append(t_ms)

            if t_ms>max_time_ms:
                print("Finished parsing file, as max time reached!")
                break
            x[t_ms] = [float(w) for w in words[2:2+points]]
            y[t_ms] = [float(w) for w in words[3+points:3+2*points]]
            z[t_ms] = [float(w) for w in words[4+2*points:]]

            print("======   L%i: at time: %s found %i points: [(%s,%s,%s),...]"%(line_num,t_ms, points,x[t_ms][0],y[t_ms][0],z[t_ms][0]))

    info = "Loaded: %s points from %s, saving %i frames"%(line_num,pos_file_name,num_frames)   
    print(info)
    
    wcon = open(wcon_file_name,'w')
    
    wcon.write('''{
    "units":{ "t":"s", "x":"um", "y":"um"},
    "comment":"Saved from Sibernetic data.",
    "note":"%s",
    "data":[\n'''%info)
    
    for t in ts:
        wcon.write('''
           {"id":1, "t":%s,
            "x":[%s], 
            "y":[%s]
	       },\n'''%(t,x[t],y[t]))

    wcon.write('}\n')
    
    wcon.close()

if __name__ == '__main__':
    

    if len(sys.argv) == 2:
        pos_file_name = sys.argv[1]
    else:
        pos_file_name = 'crawling_at_agar.txt'


    generate_wcon(pos_file_name,"sibernetic_test.wcon",rate_to_plot=20)